package com.example.course.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.course.dto.CourseResponse;
import com.example.course.entity.Course;
import com.example.course.exception.CourseFullException;
import com.example.course.exception.CourseNotFoundException;
import com.example.course.repository.CourseRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CourseService implements CommandLineRunner {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(String... args) {
        if (courseRepository.count() != 0) {
            return;
        }

        courseRepository.save(new Course("Microservices", 30, 3));
        courseRepository.save(new Course("Cloud Computing", 1, 4));
        courseRepository.save(new Course("Full Capacity Course", 0, 2));
        courseRepository.save(new Course("Database Systems", 20, 3));

        courseRepository.findAll().forEach(course ->
                log.info("Seeded course id={} name={} capacity={}",
                        course.getId(), course.getCourseName(), course.getCapacity()));
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .map(CourseResponse::from)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    @Transactional
    public CourseResponse reserve(Long courseId) {
        int updated = courseRepository.reserveSeat(courseId);
        if (updated == 0) {
            if (courseRepository.existsById(courseId)) {
                throw new CourseFullException(courseId);
            }
            throw new CourseNotFoundException(courseId);
        }
        return getCourse(courseId);
    }

    @Transactional
    public CourseResponse release(Long courseId) {
        int updated = courseRepository.releaseSeat(courseId);
        if (updated == 0) {
            throw new CourseNotFoundException(courseId);
        }
        return getCourse(courseId);
    }
}
