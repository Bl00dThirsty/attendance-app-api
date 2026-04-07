package com.example.attendance_app.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Handle resource-not-found errors
            ----------------------------------------------------------------
            @parameter: ResourceNotFoundException ex, HttpServletRequest request
            @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Handle domain conflict errors
            ----------------------------------------------------------------
            @parameter: ConflictException ex, HttpServletRequest request
            @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Handle explicit bad-request errors
            ----------------------------------------------------------------
            @parameter: BadRequestException ex, HttpServletRequest request
            @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Handle bean validation errors on DTO payloads
            ----------------------------------------------------------------
            @parameter: MethodArgumentNotValidException ex, HttpServletRequest request
            @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // Keep the first message per field to avoid noisy duplicated keys.
            validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            request.getRequestURI(),
            validationErrors
        );
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Handle validator constraint violations
    ----------------------------------------------------------------
    @parameter: ConstraintViolationException ex, HttpServletRequest request
    @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Handle unreadable or malformed JSON payloads
    ----------------------------------------------------------------
    @parameter: HttpMessageNotReadableException ex, HttpServletRequest request
    @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request payload", request.getRequestURI(), null);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Handle low-level database integrity conflicts
    ----------------------------------------------------------------
    @parameter: DataIntegrityViolationException ex, HttpServletRequest request
    @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Request conflicts with existing data", request.getRequestURI(), null);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Fallback handler for unexpected server errors
    ----------------------------------------------------------------
    @parameter: Exception ex, HttpServletRequest request
    @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandled(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request.getRequestURI(), null);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Build a normalized API error payload
    ----------------------------------------------------------------
    @parameter: status, message, path, validationErrors
    @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private ResponseEntity<ApiErrorResponse> buildResponse(
        HttpStatus status,
        String message,
        String path,
        Map<String, String> validationErrors
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            validationErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}
