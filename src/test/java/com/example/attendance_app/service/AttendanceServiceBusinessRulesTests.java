package com.example.attendance_app.service;

import com.example.attendance_app.config.AttendanceRulesProperties;
import com.example.attendance_app.dto.attendance.AttendanceCheckInRequest;
import com.example.attendance_app.entity.AttendanceSource;
import com.example.attendance_app.entity.CompanySite;
import com.example.attendance_app.entity.Employee;
import com.example.attendance_app.repository.AttendanceAuditTrailRepository;
import com.example.attendance_app.exception.BadRequestException;
import com.example.attendance_app.repository.AttendanceIdempotencyKeyRepository;
import com.example.attendance_app.repository.AttendanceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttendanceServiceBusinessRulesTests {

    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        AttendanceAuditTrailRepository attendanceAuditTrailRepository = mock(AttendanceAuditTrailRepository.class);
        AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository = mock(AttendanceIdempotencyKeyRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        SiteService siteService = mock(SiteService.class);

        when(attendanceRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Employee employee = new Employee();
        employee.setEmployeeCode("EMP-001");
        employee.setActive(true);
        when(employeeService.getEmployeeEntity(1L)).thenReturn(employee);

        CompanySite site = new CompanySite();
        site.setCode("SITE-001");
        site.setActive(true);
        site.setTimezone("Africa/Douala");
        when(siteService.getSiteEntity(1L)).thenReturn(site);

        AttendanceRulesProperties rules = new AttendanceRulesProperties();
        rules.setMaxRetroactiveMinutes(720);
        rules.setMaxFutureToleranceSeconds(0);
        rules.setDuplicateGuardEnabled(false);
        rules.setEnforceDailyWindow(true);
        rules.setDailyWindowStart(java.time.LocalTime.of(8, 0));
        rules.setDailyWindowEnd(java.time.LocalTime.of(18, 0));

        Clock fixedClock = Clock.fixed(
            Instant.parse("2026-04-08T10:00:00Z"),
            ZoneOffset.UTC
        );

        attendanceService = new AttendanceService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            attendanceIdempotencyKeyRepository,
            employeeService,
            siteService,
            rules,
            fixedClock
        );

        SecurityContextHolder.getContext().setAuthentication(buildAdminAuthentication());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectFutureArrivalTime() {
        AttendanceCheckInRequest request = buildRequest(OffsetDateTime.parse("2026-04-08T10:01:00Z"));
        assertThrows(BadRequestException.class, () -> attendanceService.recordCheckIn(request));
    }

    @Test
    void shouldRejectTooOldArrivalTime() {
        AttendanceCheckInRequest request = buildRequest(OffsetDateTime.parse("2026-04-07T22:59:59Z"));
        assertThrows(BadRequestException.class, () -> attendanceService.recordCheckIn(request));
    }

    @Test
    void shouldRejectArrivalOutsideDailyWindow() {
        AttendanceCheckInRequest request = buildRequest(OffsetDateTime.parse("2026-04-08T07:00:00+01:00"));
        assertThrows(BadRequestException.class, () -> attendanceService.recordCheckIn(request));
    }

    @Test
    void shouldAcceptArrivalWithinRulesUsingSiteTimezone() {
        AttendanceCheckInRequest request = buildRequest(OffsetDateTime.parse("2026-04-08T08:30:00+01:00"));
        assertDoesNotThrow(() -> attendanceService.recordCheckIn(request));
    }

    @Test
    void shouldReturnUtcAndLocalArrivalTimes() {
        AttendanceCheckInRequest request = buildRequest(OffsetDateTime.parse("2026-04-08T08:30:00+01:00"));

        var response = attendanceService.recordCheckIn(request);

        assertEquals(Instant.parse("2026-04-08T07:30:00Z"), response.arrivalTimeUtc());
        assertEquals(OffsetDateTime.parse("2026-04-08T08:30:00+01:00"), response.arrivalTimeLocal());
        assertEquals("Africa/Douala", response.siteTimezone());
    }

    private AttendanceCheckInRequest buildRequest(OffsetDateTime arrivalTime) {
        return new AttendanceCheckInRequest(
            1L,
            1L,
            arrivalTime,
            AttendanceSource.MOBILE_APP,
            null,
            null,
            "test"
        );
    }

    private JwtAuthenticationToken buildAdminAuthentication() {
        Jwt jwt = new Jwt(
            "test-token",
            Instant.parse("2026-04-08T09:00:00Z"),
            Instant.parse("2026-04-08T11:00:00Z"),
            Map.of("alg", "HS256"),
            Map.of("sub", "admin", "roles", List.of("ADMIN"))
        );
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
