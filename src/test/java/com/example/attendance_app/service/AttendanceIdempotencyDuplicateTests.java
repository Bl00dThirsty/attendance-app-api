package com.example.attendance_app.service;

import com.example.attendance_app.config.AttendanceRulesProperties;
import com.example.attendance_app.dto.attendance.AttendanceCheckInRequest;
import com.example.attendance_app.entity.AttendanceIdempotencyKey;
import com.example.attendance_app.entity.AttendanceRecord;
import com.example.attendance_app.entity.AttendanceSource;
import com.example.attendance_app.entity.CompanySite;
import com.example.attendance_app.entity.Employee;
import com.example.attendance_app.exception.ConflictException;
import com.example.attendance_app.repository.AttendanceAuditTrailRepository;
import com.example.attendance_app.repository.AttendanceIdempotencyKeyRepository;
import com.example.attendance_app.repository.AttendanceRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttendanceIdempotencyDuplicateTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnExistingRecordForSameIdempotencyKey() {
        AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        AttendanceAuditTrailRepository attendanceAuditTrailRepository = mock(AttendanceAuditTrailRepository.class);
        AttendanceIdempotencyKeyRepository idempotencyKeyRepository = mock(AttendanceIdempotencyKeyRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        SiteService siteService = mock(SiteService.class);

        AttendanceService service = buildService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            idempotencyKeyRepository,
            employeeService,
            siteService,
            defaultRules()
        );

        Employee employee = buildEmployee(1L, "EMP-001");
        CompanySite site = buildSite(1L, "SITE-001");
        when(employeeService.getEmployeeEntity(1L)).thenReturn(employee);
        when(siteService.getSiteEntity(1L)).thenReturn(site);

        AttendanceRecord existingRecord = buildAttendanceRecord(99L, employee, site, Instant.parse("2026-04-08T08:30:00Z"));
        AttendanceIdempotencyKey existingKey = new AttendanceIdempotencyKey();
        existingKey.setIdempotencyKey("request-123");
        existingKey.setEmployee(employee);
        existingKey.setAttendanceRecord(existingRecord);
        existingKey.setRequestFingerprint("eb945978fbad7064d065d63cee0b9e7d5c703a6454a9106ef47aca350ac755ce");
        when(idempotencyKeyRepository.findByIdempotencyKey("request-123")).thenReturn(Optional.of(existingKey));

        SecurityContextHolder.getContext().setAuthentication(buildAdminAuthentication());

        AttendanceCheckInRequest request = new AttendanceCheckInRequest(
            1L,
            1L,
            OffsetDateTime.parse("2026-04-08T09:30:00+01:00"),
            AttendanceSource.MOBILE_APP,
            null,
            null,
            ""
        );

        var response = service.recordCheckIn(request, "request-123");
        assertEquals(99L, response.id());
        verify(attendanceRecordRepository, never()).save(any());
    }

    @Test
    void shouldRejectIdempotencyKeyReuseWithDifferentPayload() {
        AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        AttendanceAuditTrailRepository attendanceAuditTrailRepository = mock(AttendanceAuditTrailRepository.class);
        AttendanceIdempotencyKeyRepository idempotencyKeyRepository = mock(AttendanceIdempotencyKeyRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        SiteService siteService = mock(SiteService.class);

        AttendanceService service = buildService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            idempotencyKeyRepository,
            employeeService,
            siteService,
            defaultRules()
        );

        Employee employee = buildEmployee(1L, "EMP-001");
        CompanySite site = buildSite(1L, "SITE-001");
        when(employeeService.getEmployeeEntity(1L)).thenReturn(employee);
        when(siteService.getSiteEntity(1L)).thenReturn(site);

        AttendanceIdempotencyKey existingKey = new AttendanceIdempotencyKey();
        existingKey.setIdempotencyKey("request-123");
        existingKey.setEmployee(employee);
        existingKey.setAttendanceRecord(buildAttendanceRecord(100L, employee, site, Instant.parse("2026-04-08T08:30:00Z")));
        existingKey.setRequestFingerprint("different-fingerprint");
        when(idempotencyKeyRepository.findByIdempotencyKey("request-123")).thenReturn(Optional.of(existingKey));

        SecurityContextHolder.getContext().setAuthentication(buildAdminAuthentication());

        AttendanceCheckInRequest request = new AttendanceCheckInRequest(
            1L,
            1L,
            OffsetDateTime.parse("2026-04-08T09:30:00+01:00"),
            AttendanceSource.MOBILE_APP,
            null,
            null,
            ""
        );

        assertThrows(ConflictException.class, () -> service.recordCheckIn(request, "request-123"));
    }

    @Test
    void shouldRejectDuplicateCheckInSameDayWhenEnabled() {
        AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        AttendanceAuditTrailRepository attendanceAuditTrailRepository = mock(AttendanceAuditTrailRepository.class);
        AttendanceIdempotencyKeyRepository idempotencyKeyRepository = mock(AttendanceIdempotencyKeyRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        SiteService siteService = mock(SiteService.class);

        AttendanceRulesProperties rules = defaultRules();
        rules.setSingleCheckInPerDay(true);
        rules.setMinMinutesBetweenCheckIns(0);

        AttendanceService service = buildService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            idempotencyKeyRepository,
            employeeService,
            siteService,
            rules
        );

        Employee employee = buildEmployee(1L, "EMP-001");
        CompanySite site = buildSite(1L, "SITE-001");
        when(employeeService.getEmployeeEntity(1L)).thenReturn(employee);
        when(siteService.getSiteEntity(1L)).thenReturn(site);

        AttendanceRecord duplicate = buildAttendanceRecord(200L, employee, site, Instant.parse("2026-04-08T07:50:00Z"));
        when(attendanceRecordRepository.findTopByEmployeeIdAndIdNotAndArrivalTimeBetweenOrderByArrivalTimeDesc(
            any(),
            any(),
            any(),
            any()
        )).thenReturn(Optional.of(duplicate));

        SecurityContextHolder.getContext().setAuthentication(buildAdminAuthentication());

        AttendanceCheckInRequest request = new AttendanceCheckInRequest(
            1L,
            1L,
            OffsetDateTime.parse("2026-04-08T09:30:00+01:00"),
            AttendanceSource.MOBILE_APP,
            null,
            null,
            ""
        );

        assertThrows(ConflictException.class, () -> service.recordCheckIn(request, null));
    }

    @Test
    void shouldRejectDuplicateCheckInWithinConfiguredInterval() {
        AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        AttendanceAuditTrailRepository attendanceAuditTrailRepository = mock(AttendanceAuditTrailRepository.class);
        AttendanceIdempotencyKeyRepository idempotencyKeyRepository = mock(AttendanceIdempotencyKeyRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        SiteService siteService = mock(SiteService.class);

        AttendanceRulesProperties rules = defaultRules();
        rules.setSingleCheckInPerDay(false);
        rules.setMinMinutesBetweenCheckIns(30);

        AttendanceService service = buildService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            idempotencyKeyRepository,
            employeeService,
            siteService,
            rules
        );

        Employee employee = buildEmployee(1L, "EMP-001");
        CompanySite site = buildSite(1L, "SITE-001");
        when(employeeService.getEmployeeEntity(1L)).thenReturn(employee);
        when(siteService.getSiteEntity(1L)).thenReturn(site);

        AttendanceRecord duplicate = buildAttendanceRecord(300L, employee, site, Instant.parse("2026-04-08T08:20:00Z"));
        when(attendanceRecordRepository.findTopByEmployeeIdAndIdNotAndArrivalTimeBetweenOrderByArrivalTimeDesc(
            any(),
            any(),
            any(),
            any()
        )).thenReturn(Optional.of(duplicate));

        SecurityContextHolder.getContext().setAuthentication(buildAdminAuthentication());

        AttendanceCheckInRequest request = new AttendanceCheckInRequest(
            1L,
            1L,
            OffsetDateTime.parse("2026-04-08T09:30:00+01:00"),
            AttendanceSource.MOBILE_APP,
            null,
            null,
            ""
        );

        assertThrows(ConflictException.class, () -> service.recordCheckIn(request, null));
    }

    private AttendanceService buildService(
        AttendanceRecordRepository attendanceRecordRepository,
        AttendanceAuditTrailRepository attendanceAuditTrailRepository,
        AttendanceIdempotencyKeyRepository idempotencyKeyRepository,
        EmployeeService employeeService,
        SiteService siteService,
        AttendanceRulesProperties rules
    ) {
        when(attendanceRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(idempotencyKeyRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        return new AttendanceService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            idempotencyKeyRepository,
            employeeService,
            siteService,
            rules,
            Clock.fixed(Instant.parse("2026-04-08T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    private AttendanceRulesProperties defaultRules() {
        AttendanceRulesProperties rules = new AttendanceRulesProperties();
        rules.setMaxRetroactiveMinutes(120);
        rules.setMaxFutureToleranceSeconds(0);
        rules.setEnforceDailyWindow(false);
        rules.setDuplicateGuardEnabled(true);
        rules.setSingleCheckInPerDay(true);
        rules.setMinMinutesBetweenCheckIns(0);
        return rules;
    }

    private Employee buildEmployee(Long id, String employeeCode) {
        Employee employee = new Employee();
        ReflectionTestUtils.setField(employee, "id", id);
        employee.setEmployeeCode(employeeCode);
        employee.setActive(true);
        return employee;
    }

    private CompanySite buildSite(Long id, String code) {
        CompanySite site = new CompanySite();
        ReflectionTestUtils.setField(site, "id", id);
        site.setCode(code);
        site.setActive(true);
        site.setTimezone("Africa/Douala");
        return site;
    }

    private AttendanceRecord buildAttendanceRecord(Long id, Employee employee, CompanySite site, Instant arrivalTime) {
        AttendanceRecord record = new AttendanceRecord();
        ReflectionTestUtils.setField(record, "id", id);
        record.setEmployee(employee);
        record.setSite(site);
        record.setArrivalTime(arrivalTime);
        record.setCheckInSource(AttendanceSource.MOBILE_APP);
        record.setStatus(com.example.attendance_app.entity.AttendanceStatus.UNVERIFIED);
        return record;
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
