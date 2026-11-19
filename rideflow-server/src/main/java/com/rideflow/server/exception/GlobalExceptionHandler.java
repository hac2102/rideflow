package com.rideflow.server.exception;

import com.rideflow.core.exception.DoubleBookingException;
import com.rideflow.core.exception.DriverUnavailableException;
import com.rideflow.core.exception.InvalidCoordinateException;
import com.rideflow.core.exception.InvalidRideStateException;
import com.rideflow.core.exception.PricingConfigurationException;
import com.rideflow.core.exception.RideNotFoundException;
import com.rideflow.server.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<ErrorResponseDto> handleNotFound(RideNotFoundException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.of(404, "RIDE_NOT_FOUND", e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidCoordinateException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCoordinate(InvalidCoordinateException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(400, "INVALID_COORDINATE", e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidRideStateException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRideState(InvalidRideStateException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(400, "INVALID_RIDE_STATE", e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DriverUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleDriverUnavailable(DriverUnavailableException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(400, "DRIVER_UNAVAILABLE", e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DoubleBookingException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(DoubleBookingException e, HttpServletRequest request) {
        log.warn("Concurrency conflict prevented: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.of(409, "DOUBLE_BOOKING_CONFLICT", e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(PricingConfigurationException.class)
    public ResponseEntity<ErrorResponseDto> handlePricingConfig(PricingConfigurationException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(400, "PRICING_CONFIG_ERROR", e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(400, "VALIDATION_ERROR", msg, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception e, HttpServletRequest request) {
        log.error("Unhandled internal server error on path {}", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.of(500, "INTERNAL_SERVER_ERROR", e.getMessage(), request.getRequestURI()));
    }
}
