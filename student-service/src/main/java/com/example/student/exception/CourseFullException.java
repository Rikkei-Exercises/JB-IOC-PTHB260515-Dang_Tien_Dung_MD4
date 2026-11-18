package com.example.student.exception;

public class CourseFullException extends RuntimeException {
    public CourseFullException(Long courseId) {
        super("Course is full");
    }

    public CourseFullException(String message) {
        super(message);
    }
}
