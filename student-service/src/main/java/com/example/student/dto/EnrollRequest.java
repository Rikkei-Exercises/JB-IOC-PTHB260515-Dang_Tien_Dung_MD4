package com.example.student.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollRequest(
        @NotNull(message = "studentId is required")
        Long studentId,

        @NotNull(message = "courseId is required")
        Long courseId
) {
}
