package com.example.course.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.course.dto.CourseResponse;
import com.example.course.service.CourseService;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/{courseId}")
    public CourseResponse getCourse(@PathVariable Long courseId) {
        return courseService.getCourse(courseId);
    }

    @PutMapping("/{courseId}/reserve")
    public CourseResponse reserve(@PathVariable Long courseId) {
        return courseService.reserve(courseId);
    }

    @PutMapping("/{courseId}/release")
    public CourseResponse release(@PathVariable Long courseId) {
        return courseService.release(courseId);
    }
}
