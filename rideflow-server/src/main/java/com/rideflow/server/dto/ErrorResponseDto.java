package com.rideflow.server.dto;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        int status,
        String errorCode,
        String message,
        LocalDateTime timestamp,
        String path
) {
    public static ErrorResponseDto of(int status, String errorCode, String message, String path) {
        return new ErrorResponseDto(status, errorCode, message, LocalDateTime.now(), path);
    }
}
