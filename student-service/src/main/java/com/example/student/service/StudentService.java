package com.example.student.service;

import java.util.function.Supplier;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.student.client.CourseClient;
import com.example.student.dto.CourseResponse;
import com.example.student.dto.EnrollRequest;
import com.example.student.dto.StudentResponse;
import com.example.student.entity.Student;
import com.example.student.exception.AlreadyEnrolledException;
import com.example.student.exception.CourseFullException;
import com.example.student.exception.CourseNotFoundException;
import com.example.student.exception.CourseUnavailableException;
import com.example.student.exception.EnrollmentFailedException;
import com.example.student.exception.StudentNotFoundException;
import com.example.student.repository.StudentRepository;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StudentService implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final CourseClient courseClient;

    public StudentService(StudentRepository studentRepository, CourseClient courseClient) {
        this.studentRepository = studentRepository;
        this.courseClient = courseClient;
    }

    @Override
    public void run(String... args) {
        if (studentRepository.count() != 0) {
            return;
        }

        // Seed ba sinh viên mẫu khi bảng rỗng.
        Student first = studentRepository.save(new Student("Nguyen Van A", "nguyenvana@example.com"));
        Student second = studentRepository.save(new Student("Tran Thi B", "tranthib@example.com"));
        Student third = studentRepository.save(new Student("Le Van C", "levanc@example.com"));

        log.info("Seeded student id={} name={} email={}", first.getId(), first.getName(), first.getEmail());
        log.info("Seeded student id={} name={} email={}", second.getId(), second.getName(), second.getEmail());
        log.info("Seeded student id={} name={} email={}", third.getId(), third.getName(), third.getEmail());
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .map(StudentResponse::from)
                .orElseThrow(() -> new StudentNotFoundException("Student not found"));
    }

    @Transactional
    public StudentResponse enroll(EnrollRequest request) {
        Long studentId = request.studentId();
        Long courseId = request.courseId();

        // Khóa dòng sinh viên trong lúc kiểm tra trùng và cập nhật đăng ký.
        Student student = studentRepository.findByIdWithLock(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found"));

        if (student.getEnrolledCourses().contains(courseId)) {
            throw new AlreadyEnrolledException("Already enrolled");
        }

        CourseResponse course = callCourse(() -> courseClient.getCourse(courseId));
        if (course.capacity() == null || course.capacity() <= 0) {
            throw new CourseFullException("Course is full");
        }

        callCourse(() -> courseClient.reserveCourse(courseId));

        try {
            student.getEnrolledCourses().add(courseId);
            student = studentRepository.saveAndFlush(student);
        } catch (RuntimeException ex) {
            log.error("Could not save enrollment for student {} and course {}; releasing reserved seat",
                    studentId, courseId, ex);
            // Hoàn chỗ đã giữ nếu không lưu được đăng ký của sinh viên.
            releaseCourseSafe(courseId);
            throw new EnrollmentFailedException("Failed to complete enrollment");
        }

        return StudentResponse.from(student);
    }

    private <T> T callCourse(Supplier<T> request) {
        try {
            return request.get();
        } catch (FeignException.NotFound ex) {
            throw new CourseNotFoundException("Course not found");
        } catch (FeignException.BadRequest ex) {
            throw new CourseFullException("Course is full");
        } catch (FeignException ex) {
            log.warn("Course Service request failed: {}", ex.getMessage());
            throw new CourseUnavailableException("Course service is unavailable");
        }
    }

    private void releaseCourseSafe(Long courseId) {
        try {
            callCourse(() -> courseClient.releaseCourse(courseId));
            log.info("Released reserved seat for course {}", courseId);
        } catch (RuntimeException ex) {
            log.error("Failed to release reserved seat for course {}", courseId, ex);
        }
    }
}
