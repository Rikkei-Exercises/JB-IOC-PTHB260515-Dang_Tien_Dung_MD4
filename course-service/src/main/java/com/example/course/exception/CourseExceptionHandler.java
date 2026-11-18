package com.example.course.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CourseExceptionHandler {

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(CourseNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Course not found");
    }

    @ExceptionHandler(CourseFullException.class)
    public ResponseEntity<ApiError> handleFull(CourseFullException exception) {
        return error(HttpStatus.BAD_REQUEST, "Course is full");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String error) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), error));
    }
}
