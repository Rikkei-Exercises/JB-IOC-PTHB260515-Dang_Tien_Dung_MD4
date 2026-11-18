package com.example.course.exception;

public class CourseFullException extends RuntimeException {

    public CourseFullException(Long courseId) {
        super("Course " + courseId + " is full");
    }
}
