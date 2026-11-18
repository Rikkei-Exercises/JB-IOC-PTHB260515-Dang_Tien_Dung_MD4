package com.example.course.dto;

import com.example.course.entity.Course;

public record CourseResponse(Long id, String courseName, Integer capacity, Integer credits) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCourseName(),
                course.getCapacity(),
                course.getCredits());
    }
}
