package com.bookingapi.common.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return of(HttpStatus.OK, null, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return of(HttpStatus.OK, message, data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return of(HttpStatus.CREATED, message, data);
    }

    public static ApiResponse<Void> message(String message) {
        return of(HttpStatus.OK, message, null);
    }

    public ResponseEntity<ApiResponse<T>> toResponseEntity() {
        return ResponseEntity.status(status).body(this);
    }

    private static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
        return new ApiResponse<>(true, status.value(), message, data, Instant.now());
    }
}