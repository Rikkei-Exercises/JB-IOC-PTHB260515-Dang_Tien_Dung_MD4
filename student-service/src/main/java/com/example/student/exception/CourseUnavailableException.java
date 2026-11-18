package com.example.student.exception;

public class CourseUnavailableException extends RuntimeException {
    public CourseUnavailableException(String message) {
        super(message);
    }
}
