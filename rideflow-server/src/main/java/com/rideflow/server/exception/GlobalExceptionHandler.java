package com.rideflow.server.exception;

import com.rideflow.core.exception.DoubleBookingException;
import com.rideflow.core.exception.DriverUnavailableException;
import com.rideflow.core.exception.InvalidCoordinateException;
import com.rideflow.core.exception.InvalidRideStateException;
import com.rideflow.core.exception.RideNotFoundException;
import com.rideflow.server.dto.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RideNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(RideNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.of(404, "Not Found", e.getMessage()));
    }

    @ExceptionHandler({InvalidRideStateException.class, DriverUnavailableException.class, InvalidCoordinateException.class})
    public ResponseEntity<ErrorResponseDto> handleBadRequest(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(400, "Bad Request", e.getMessage()));
    }

    @ExceptionHandler(DoubleBookingException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(DoubleBookingException e) {
        log.warn("Concurrency conflict prevented: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.of(409, "Conflict", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(400, "Validation Error", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception e) {
        log.error("Unhandled internal server error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.of(500, "Internal Server Error", e.getMessage()));
    }
}
