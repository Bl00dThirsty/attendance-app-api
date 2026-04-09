package com.example.attendance_app.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerObservabilityTests {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldIncludeTraceIdInErrorPayload() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/attendance/check-in");
        MDC.put("traceId", "abc123trace");

        var response = handler.handleBadRequest(new BadRequestException("invalid input"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("abc123trace", response.getBody().traceId());
        assertNotNull(response.getBody().timestamp());
    }
}
