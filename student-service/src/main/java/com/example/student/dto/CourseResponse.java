package com.example.student.dto;

public record CourseResponse(
        Long id,
        String courseName,
        Integer capacity,
        Integer credits
) {
}
