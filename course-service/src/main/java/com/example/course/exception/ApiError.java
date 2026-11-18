package com.example.course.exception;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error) {
}
