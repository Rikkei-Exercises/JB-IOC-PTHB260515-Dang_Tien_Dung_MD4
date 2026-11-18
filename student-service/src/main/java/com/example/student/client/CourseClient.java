package com.example.student.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import com.example.student.dto.CourseResponse;

@FeignClient(name = "course-service")
public interface CourseClient {

    @GetMapping("/api/courses/{courseId}")
    CourseResponse getCourse(@PathVariable("courseId") Long courseId);

    @PutMapping("/api/courses/{courseId}/reserve")
    CourseResponse reserveCourse(@PathVariable("courseId") Long courseId);

    @PutMapping("/api/courses/{courseId}/release")
    CourseResponse releaseCourse(@PathVariable("courseId") Long courseId);
}
