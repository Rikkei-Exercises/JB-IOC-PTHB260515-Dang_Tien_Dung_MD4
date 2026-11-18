package com.example.course.exception;

public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(Long courseId) {
        super("Course " + courseId + " was not found");
    }
}
