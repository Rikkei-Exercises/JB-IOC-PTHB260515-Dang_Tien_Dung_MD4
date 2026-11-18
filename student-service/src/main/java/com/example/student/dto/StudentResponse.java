package com.example.student.dto;

import java.util.ArrayList;
import java.util.List;

import com.example.student.entity.Student;

public record StudentResponse(
        Long id,
        String name,
        String email,
        List<Long> enrolledCourses
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getEnrolledCourses() != null ? new ArrayList<>(student.getEnrolledCourses()) : new ArrayList<>()
        );
    }
}
