package com.example.attendance_app.config;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpAccessLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAccessLoggingFilter.class);
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final boolean enabled;
    private final ObjectProvider<Tracer> tracerProvider;

    public HttpAccessLoggingFilter(
        @Value("${app.observability.http-access-log-enabled:true}") boolean enabled,
        ObjectProvider<Tracer> tracerProvider
    ) {
        this.enabled = enabled;
        this.tracerProvider = tracerProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        long startTimeNs = System.nanoTime();
        String requestId = resolveRequestId(request);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startTimeNs) / 1_000_000;
            String traceId = resolveTraceId();
            String spanId = resolveSpanId();

            LOGGER.info(
                "http_request_completed method={} path={} status={} durationMs={} clientIp={} userAgent={} requestId={} traceId={} spanId={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMs,
                resolveClientIp(request),
                normalizeBlank(request.getHeader("User-Agent")),
                requestId,
                traceId,
                spanId
            );

            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String headerValue = normalizeBlank(request.getHeader(REQUEST_ID_HEADER));
        return headerValue == null ? UUID.randomUUID().toString() : headerValue;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = normalizeBlank(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor;
        }
        return normalizeBlank(request.getRemoteAddr());
    }

    private String resolveTraceId() {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null || tracer.currentSpan() == null) {
            return normalizeBlank(MDC.get("traceId"));
        }
        return tracer.currentSpan().context().traceId();
    }

    private String resolveSpanId() {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null || tracer.currentSpan() == null) {
            return normalizeBlank(MDC.get("spanId"));
        }
        return tracer.currentSpan().context().spanId();
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
