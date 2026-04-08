package com.example.attendance_app.service;

import com.example.attendance_app.config.AttendanceRulesProperties;
import com.example.attendance_app.dto.attendance.AttendanceCheckInRequest;
import com.example.attendance_app.entity.AttendanceSource;
import com.example.attendance_app.entity.CompanySite;
import com.example.attendance_app.entity.Employee;
import com.example.attendance_app.repository.AttendanceAuditTrailRepository;
import com.example.attendance_app.repository.AttendanceIdempotencyKeyRepository;
import com.example.attendance_app.repository.AttendanceRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttendanceSelfCheckInAuthorizationTests {

    private AttendanceService attendanceService;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        AttendanceAuditTrailRepository attendanceAuditTrailRepository = mock(AttendanceAuditTrailRepository.class);
        AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository = mock(AttendanceIdempotencyKeyRepository.class);
        employeeService = mock(EmployeeService.class);
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
        rules.setMaxRetroactiveMinutes(120);
        rules.setMaxFutureToleranceSeconds(0);
        rules.setDuplicateGuardEnabled(false);
        rules.setEnforceDailyWindow(false);

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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void employeeRoleShouldAllowOwnCheckInUsingEmployeeIdClaim() {
        SecurityContextHolder.getContext().setAuthentication(buildEmployeeAuthentication(Map.of("employeeId", 1L)));
        AttendanceCheckInRequest request = buildRequest(1L);

        assertDoesNotThrow(() -> attendanceService.recordCheckIn(request));
    }

    @Test
    void employeeRoleShouldRejectCheckInForAnotherEmployee() {
        SecurityContextHolder.getContext().setAuthentication(buildEmployeeAuthentication(Map.of("employeeId", 2L)));
        AttendanceCheckInRequest request = buildRequest(1L);

        assertThrows(AccessDeniedException.class, () -> attendanceService.recordCheckIn(request));
    }

    @Test
    void employeeRoleShouldResolveIdentityFromSubjectFallback() {
        when(employeeService.resolveEmployeeIdByIdentifier("emp.user")).thenReturn(Optional.of(1L));
        SecurityContextHolder.getContext().setAuthentication(buildEmployeeAuthentication(Map.of("sub", "emp.user")));
        AttendanceCheckInRequest request = buildRequest(1L);

        assertDoesNotThrow(() -> attendanceService.recordCheckIn(request));
    }

    @Test
    void employeeRoleShouldRejectWhenNoIdentityClaimCanBeResolved() {
        SecurityContextHolder.getContext().setAuthentication(buildEmployeeAuthentication(Map.of("sub", "unknown")));
        when(employeeService.resolveEmployeeIdByIdentifier("unknown")).thenReturn(Optional.empty());
        AttendanceCheckInRequest request = buildRequest(1L);

        assertThrows(AccessDeniedException.class, () -> attendanceService.recordCheckIn(request));
    }

    @Test
    void adminRoleShouldBypassSelfCheckRestriction() {
        SecurityContextHolder.getContext().setAuthentication(buildAdminAuthentication());
        AttendanceCheckInRequest request = buildRequest(1L);

        assertDoesNotThrow(() -> attendanceService.recordCheckIn(request));
    }

    private AttendanceCheckInRequest buildRequest(Long employeeId) {
        return new AttendanceCheckInRequest(
            employeeId,
            1L,
            OffsetDateTime.parse("2026-04-08T09:30:00+01:00"),
            AttendanceSource.MOBILE_APP,
            null,
            null,
            "test"
        );
    }

    private JwtAuthenticationToken buildEmployeeAuthentication(Map<String, Object> customClaims) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("roles", List.of("EMPLOYEE"));
        claims.putAll(customClaims);
        claims.putIfAbsent("sub", "employee.user");

        Jwt jwt = new Jwt(
            "employee-token",
            Instant.parse("2026-04-08T09:00:00Z"),
            Instant.parse("2026-04-08T11:00:00Z"),
            Map.of("alg", "HS256"),
            claims
        );
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
    }

    private JwtAuthenticationToken buildAdminAuthentication() {
        Jwt jwt = new Jwt(
            "admin-token",
            Instant.parse("2026-04-08T09:00:00Z"),
            Instant.parse("2026-04-08T11:00:00Z"),
            Map.of("alg", "HS256"),
            Map.of("sub", "admin", "roles", List.of("ADMIN"))
        );
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
