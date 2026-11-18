package com.example.student.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Long studentId) {
        super("Student not found");
    }

    public StudentNotFoundException(String message) {
        super(message);
    }
}
