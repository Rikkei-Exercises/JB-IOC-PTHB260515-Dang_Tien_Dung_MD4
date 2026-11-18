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


## Task 3 — Phân tích kiến trúc

### 1. Giải thích cơ chế Service Discovery: Tại sao không gọi trực tiếp IP/P/Port?

- Service Discovery là cơ chế giúp các service tìm nhau bằng tên thay vì địa chỉ cố định.
- Trong hệ thống này, Student Service, Course Service và API Gateway tự đăng ký vào Eureka Server khi khởi động và gửi heartbeat định kỳ.
Khi nhận request /api/students/**, Gateway hỏi Eureka địa chỉ của STUDENT-SERVICE (thông qua lb://STUDENT-SERVICE) sau đó chuyển request đi.
Tương tự, Student Service gọi Course Service qua OpenFeign bằng tên course-service.

- Gateway không nên gọi trực tiếp IP/Port vì địa chỉ này sẽ thay đổi khi restart hoặc khi deloy.
không phát hiện được service đã chết và buộc phải sửa cấu hình mỗi khi địa chỉ thay đổi.
Nhờ Service Discovery, Gateway chỉ cần tên service -> hệ thống linh hoạt và dễ mở rộng hơn.

### 2. Nếu số lượng request tăng đột biến, làm thế nào để scale Student Service mà không thay đổicấu hình Gateway?

- Để scale Student Service khi lượng request tăng đột biến chạy thêm nhiều instance của service này (ví dụ khác port) thì 
tất cả instance dùng cùng spring.application.name=student-service nên tự đăng ký vào Eureka dưới cùng một tên STUDENT-SERVICE.
Gateway định tuyến bằng lb://STUDENT-SERVICE, nó lấy danh sách instance từ Eureka nên không cần thay đổi bất kỳ cấu hình nào của Gateway.
Instance nào ngừng hoạt động sẽ bị Eureka gỡ khỏi danh bạ.
- Để việc này đúng đắn, Student Service phải stateless và các instance dùng chung student_db.
Luồng đăng ký dùng khóa dòng khi đọc sinh viên và Course Service giảm capacity bằng một câu lệnh có điều kiện,
nhờ đó nhiều instance xử lý song song vẫn không đăng ký trùng hoặc vượt sức chứa.

### 3. So sánh ưu/nhược điểm OpenFeign (Đồng bộ) vs Kafka (Bất đồng bộ)

* OpenFeign (đồng bộ): Student Service gọi Course Service và chờ kết quả trong cùng request.
  - Ưu điểm: Đơn giản, người dùng nhận kết quả tức thì (thành công, hết chỗ, không tồn tại), dễ xử lý lỗi.
  - Nhược điểm: Nếu Course Service ngừng hoạt động thì việc đăng ký thất bại; khi tải tăng đột biến Student Service dễ bị nghẽn vì phải chờ Course.

* Kafka (bất đồng bộ): Student Service gửi message đến Course Service sẽ nhận thông tin, nhưng không chờ kết quả ngay mà trả về cho người dùng thông báo "đang xử lý".
sau đó course Service sẽ xử lý message và gửi kết quả đến Student Service.
  - Ưu điểm: Student Service không bị nghẽn khi Course Service chậm, hệ thống có thể xử lý nhiều request hơn, dễ mở rộng.
  - Nhược điểm: Phức tạp hơn, người dùng không nhận kết quả ngay, cần cơ chế retry và đảm bảo message được xử lý đúng, khó debug hơn
.

