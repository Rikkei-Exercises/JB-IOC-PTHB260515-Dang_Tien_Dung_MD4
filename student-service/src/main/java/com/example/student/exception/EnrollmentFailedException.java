package com.example.student.exception;

public class EnrollmentFailedException extends RuntimeException {
    public EnrollmentFailedException(String message) {
        super(message);
    }
}
