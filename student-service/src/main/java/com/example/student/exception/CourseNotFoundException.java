package com.example.student.exception;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(Long courseId) {
        super("Course not found");
    }

    public CourseNotFoundException(String message) {
        super(message);
    }
}
