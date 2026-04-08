package com.example.attendance_app.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigCorsPolicyTests {

    private static final String TEST_JWT_SECRET = "YXR0ZW5kYW5jZS10ZXN0LXNlY3JldC1rZXktMzItYnl0ZXM=";

    @Test
    void securePolicyRejectsLocalhostOrigins() {
        assertThrows(
            IllegalStateException.class,
            () -> new SecurityConfig(
                TEST_JWT_SECRET,
                "cale-auth-service",
                "attendance-app-api",
                List.of("http://localhost:4200"),
                List.of("GET", "POST"),
                List.of("Authorization", "Content-Type"),
                List.of("Authorization"),
                true,
                3600,
                true,
                false,
                false,
                false
            )
        );
    }

    @Test
    void securePolicyRejectsWildcardHeaders() {
        assertThrows(
            IllegalStateException.class,
            () -> new SecurityConfig(
                TEST_JWT_SECRET,
                "cale-auth-service",
                "attendance-app-api",
                List.of("https://app.cale-erp.com"),
                List.of("GET", "POST"),
                List.of("*"),
                List.of("Authorization"),
                true,
                3600,
                true,
                false,
                false,
                false
            )
        );
    }

    @Test
    void securePolicyAcceptsHttpsOrigins() {
        assertDoesNotThrow(
            () -> new SecurityConfig(
                TEST_JWT_SECRET,
                "cale-auth-service",
                "attendance-app-api",
                List.of("https://app.cale-erp.com", "https://admin.cale-erp.com"),
                List.of("GET", "POST", "OPTIONS"),
                List.of("Authorization", "Content-Type", "Accept", "Origin"),
                List.of("Authorization"),
                true,
                3600,
                true,
                false,
                false,
                false
            )
        );
    }
}
