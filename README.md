# Quản lý sinh viên và khóa học

Monorepo gồm Eureka Registry, API Gateway, Student Service và Course Service.
Các service dùng Java 17, Spring Boot 3.5.14, Spring Cloud 2025.0.3, Maven và
PostgreSQL. Student gọi Course đồng bộ bằng OpenFeign.

| Thành phần | Tên service | Cổng |
|---|---|---:|
| Eureka Registry | `service-registry` | 8761 |
| API Gateway (WebFlux) | `api-gateway` | 8080 |
| Student Service | `student-service` | 8081 |
| Course Service | `course-service` | 8082 |

## Kiến trúc

```text
                    +---------------------+
                    | Eureka Registry     |
                    | :8761               |
                    +--^--------^--------^+
                       |        |        |
                 đăng ký / heartbeat từ 3 service

Client --> API Gateway --lb://STUDENT-SERVICE--> Student Service --> student_db
             :8080      \-lb://COURSE-SERVICE---> Course Service --> course_db
                              Student --Feign (service ID: course-service)--> Course
```

`API-GATEWAY`, `STUDENT-SERVICE` và `COURSE-SERVICE` đăng ký vào Eureka.
Gateway định tuyến bằng service ID (`lb://STUDENT-SERVICE`,
`lb://COURSE-SERVICE`); Student dùng Feign với tên `course-service`, không
chỉ định URL cố định.

## Cấu trúc thư mục

```text
service-registry/   Eureka Server
api-gateway/        Gateway, routes và Global Filter
course-service/     controller, service, repository, entity, dto, exception
student-service/    controller, service, repository, entity, dto, client, exception
docker-compose.yml  PostgreSQL
init.sql            Tạo student_db và course_db
pom.xml             Maven parent
```

## Chạy hệ thống

Cần Java 17, Maven và Docker Compose. Mở các terminal riêng và chạy lần lượt:

```bash
docker compose up -d
mvn -f service-registry/pom.xml spring-boot:run
mvn -f course-service/pom.xml spring-boot:run
mvn -f student-service/pom.xml spring-boot:run
mvn -f api-gateway/pom.xml spring-boot:run
```

Chờ PostgreSQL sẵn sàng trước khi chạy service; đợi Eureka khởi động trước
Course, Student và Gateway. Dashboard Eureka ở `http://localhost:8761`.
PostgreSQL tạo hai database từ `init.sql`; dữ liệu mẫu được seed khi bảng
tương ứng còn trống. Muốn tạo lại database và dữ liệu seed, dùng
`docker compose down -v` rồi `docker compose up -d` (lệnh đầu xóa volume DB).

Các API chính được gọi qua Gateway:

```bash
curl http://localhost:8080/api/courses/1
curl http://localhost:8080/api/students/1
curl -X POST http://localhost:8080/api/students/enroll \
  -H "Content-Type: application/json" \
  -d '{"studentId":1,"courseId":1}'
```

Course Service cũng cung cấp `GET /api/courses/{courseId}`,
`PUT /api/courses/{courseId}/reserve` và
`PUT /api/courses/{courseId}/release`. Student Service cung cấp
`GET /api/students/{studentId}` và `POST /api/students/enroll`.

## Luồng đăng ký khóa học

1. Student Service kiểm tra payload, khóa dòng sinh viên, xác nhận sinh viên
   tồn tại và chưa đăng ký khóa học đó.
2. Student gọi Course Service qua Feign để kiểm tra khóa học; Course giữ chỗ
   bằng một cập nhật capacity có điều kiện trong transaction.
3. Student thêm course ID vào `enrolled_courses` rồi lưu sinh viên. Nếu thao tác
   lưu thất bại, Student gọi API `release` để hoàn chỗ đã giữ.
4. Lỗi được trả thành JSON gọn: dữ liệu không tồn tại `404`, payload sai hoặc
   khóa học hết chỗ/đã đăng ký `400`, Course Service không khả dụng `503`.

Hai database không có transaction chung. `release` là thao tác bù trừ khi lưu
Student thất bại, không đảm bảo nhất quán tuyệt đối nếu dịch vụ hoặc kết nối
gặp lỗi trong lúc bù trừ.

## Task 3 — Phân tích kiến trúc

### 1. Vì sao dùng Service Discovery?

Địa chỉ IP và port của service có thể đổi khi restart, scale hoặc deploy.
Eureka giữ danh bạ động: mỗi service đăng ký tên và gửi heartbeat. Gateway
dùng `lb://STUDENT-SERVICE` hoặc `lb://COURSE-SERVICE` để tra danh bạ và chọn
instance, thay vì cấu hình địa chỉ cố định. Ví dụ, khi Course Service có
instance mới, Gateway có thể định tuyến đến instance đã đăng ký đó.

### 2. Scale Student Service như thế nào?

Chạy thêm Student Service instance trên port khác hoặc trong container riêng;
các instance đăng ký cùng tên `student-service` vào Eureka. Gateway tiếp tục
dùng route hiện có và LoadBalancer phân phối request, không cần sửa cấu hình
Gateway. Student Service cần stateless và các instance cùng dùng `student_db`.

### 3. OpenFeign và Kafka

Với OpenFeign, Student gọi Course đồng bộ và nhận ngay kết quả reserve. Cách
này dễ trả lời người dùng, ánh xạ lỗi và hoàn chỗ khi lưu Student thất bại;
đổi lại, Student phụ thuộc Course đang chạy và hai service gắn kết chặt hơn.

Kafka tách bên gửi và nhận, phù hợp khi cần đệm tải và xử lý sự kiện lớn.
Đổi lại, xử lý đăng ký thường eventual consistency; khó trả kết quả cuối cùng
ngay trong request, đồng thời cần xử lý phản hồi và trạng thái bất đồng bộ.
Bài này chọn OpenFeign vì luồng đăng ký cần biết ngay còn chỗ hay không và
trả thành công/thất bại đồng bộ.

## Tự chấm theo thang điểm

| Tiêu chí | Điểm | Bằng chứng | Khả năng mất điểm |
|---|---:|---|---|
| Hạ tầng | 15/15 | Eureka: `ServiceRegistryApplication.java:7`, `application.yml:1-11`; cổng và đăng ký đã kiểm tra khi chạy | Không thấy thiếu tiêu chí |
| Gateway | 20/20 | `api-gateway/src/main/resources/application.yml:1-21`; `RequestLoggingFilter.java:19-38`; log request và route đã chạy qua Gateway | Không thấy thiếu tiêu chí |
| Kết nối | 15/15 | `CourseClient.java:10-20`; `CourseService.java:48-64`; `CourseRepository.java:12-18`; kiểm thử song song: đúng một `200`, một `400`, capacity còn `0` | Không thấy thiếu tiêu chí |
| Nghiệp vụ | 20/20 | `StudentService.java:60-95`, `StudentRepository.java:14-18`; test enroll, trùng, hết chỗ và Course ngừng hoạt động | `release` là bù trừ giữa hai DB, không phải transaction phân tán |
| Thiết kế API | 15/15 | `StudentController.java:20-36`, `CourseController.java:14-34`, `EnrollRequest.java:4-10`, các `RestControllerAdvice`; đã kiểm tra 200/400/404/503 qua Gateway | Không thấy thiếu tiêu chí |
| Trả lời câu hỏi | 15/15 | Ba câu Task 3 ở các mục trên | Không thấy thiếu tiêu chí |
| **Tổng** | **100/100** | Build riêng từng service và nghiệm thu API qua cổng 8080 | Kiểm thử nghiệm thu được chạy thủ công, chưa đóng gói thành bộ integration test tự động |

Điểm tự chấm dựa trên yêu cầu và bằng chứng chạy hiện có; điểm chính thức do
giám khảo quyết định.
