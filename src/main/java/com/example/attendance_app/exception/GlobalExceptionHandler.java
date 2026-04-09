package com.example.attendance_app.exception;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Handle resource-not-found errors
    ----------------------------------------------------------------
    @parameter: ResourceNotFoundException ex, HttpServletRequest request
    @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        LOGGER.warn("resource_not_found path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Handle domain conflict errors
    ----------------------------------------------------------------
    @parameter: ConflictException ex, HttpServletRequest request
    @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        LOGGER.warn("domain_conflict path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Handle explicit bad-request errors
    ----------------------------------------------------------------
    @parameter: BadRequestException ex, HttpServletRequest request
    @Returnvalue: ResponseEntity<ApiErrorResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        LOGGER.warn("bad_request path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
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
        LOGGER.warn("validation_failed path={} errors={}", request.getRequestURI(), validationErrors);

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
        LOGGER.warn("constraint_violation path={} message={}", request.getRequestURI(), ex.getMessage());
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
        LOGGER.warn("malformed_payload path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request payload", request.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        LOGGER.warn("access_denied path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI(), null);
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
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        String details = mostSpecificCause == null ? ex.getMessage() : mostSpecificCause.getMessage();
        LOGGER.warn("data_integrity_violation path={} message={}", request.getRequestURI(), details);
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
        LOGGER.error("unexpected_server_error path={}", request.getRequestURI(), ex);
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
        String traceId = MDC.get("traceId");
        ApiErrorResponse response = new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            traceId,
            validationErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}
