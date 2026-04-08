package com.example.attendance_app.service;

import com.example.attendance_app.config.AttendanceRulesProperties;
import com.example.attendance_app.dto.attendance.AttendanceCancellationRequest;
import com.example.attendance_app.dto.attendance.AttendanceCorrectionRequest;
import com.example.attendance_app.entity.AttendanceAuditAction;
import com.example.attendance_app.entity.AttendanceAuditTrail;
import com.example.attendance_app.entity.AttendanceRecord;
import com.example.attendance_app.entity.AttendanceSource;
import com.example.attendance_app.entity.AttendanceStatus;
import com.example.attendance_app.entity.CompanySite;
import com.example.attendance_app.entity.Employee;
import com.example.attendance_app.repository.AttendanceAuditTrailRepository;
import com.example.attendance_app.repository.AttendanceIdempotencyKeyRepository;
import com.example.attendance_app.repository.AttendanceRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttendanceAuditTrailTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPersistAuditEntryWhenCorrectingAttendance() {
        AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        AttendanceAuditTrailRepository attendanceAuditTrailRepository = mock(AttendanceAuditTrailRepository.class);
        AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository = mock(AttendanceIdempotencyKeyRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        SiteService siteService = mock(SiteService.class);

        AttendanceService service = buildService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            attendanceIdempotencyKeyRepository,
            employeeService,
            siteService
        );

        AttendanceRecord record = buildRecord(12L);
        when(attendanceRecordRepository.findById(12L)).thenReturn(Optional.of(record));
        when(attendanceRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(buildAdminAuthentication());

        AttendanceCorrectionRequest request = new AttendanceCorrectionRequest(
            OffsetDateTime.parse("2026-04-08T09:40:00+01:00"),
            AttendanceSource.WEB_TERMINAL,
            AttendanceStatus.ON_SITE,
            "corrected",
            "terminal import fix"
        );

        var response = service.correctAttendance(12L, request);

        assertEquals(AttendanceSource.WEB_TERMINAL, response.checkInSource());
        assertEquals(AttendanceStatus.ON_SITE, response.status());
        assertEquals("corrected", response.notes());

        ArgumentCaptor<AttendanceAuditTrail> captor = ArgumentCaptor.forClass(AttendanceAuditTrail.class);
        verify(attendanceAuditTrailRepository).save(captor.capture());
        AttendanceAuditTrail savedAudit = captor.getValue();
        assertEquals(AttendanceAuditAction.CORRECTED, savedAudit.getAction());
        assertEquals("terminal import fix", savedAudit.getReason());
        assertEquals("admin.user", savedAudit.getActorSubject());
    }

    @Test
    void shouldPersistAuditEntryWhenCancellingAttendance() {
        AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        AttendanceAuditTrailRepository attendanceAuditTrailRepository = mock(AttendanceAuditTrailRepository.class);
        AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository = mock(AttendanceIdempotencyKeyRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        SiteService siteService = mock(SiteService.class);

        AttendanceService service = buildService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            attendanceIdempotencyKeyRepository,
            employeeService,
            siteService
        );

        AttendanceRecord record = buildRecord(44L);
        when(attendanceRecordRepository.findById(44L)).thenReturn(Optional.of(record));
        when(attendanceRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(buildAdminAuthentication());

        var response = service.cancelAttendance(44L, new AttendanceCancellationRequest("invalid device sync"));
        assertEquals(AttendanceStatus.CANCELLED, response.status());

        ArgumentCaptor<AttendanceAuditTrail> captor = ArgumentCaptor.forClass(AttendanceAuditTrail.class);
        verify(attendanceAuditTrailRepository).save(captor.capture());
        AttendanceAuditTrail savedAudit = captor.getValue();
        assertEquals(AttendanceAuditAction.CANCELLED, savedAudit.getAction());
        assertEquals("invalid device sync", savedAudit.getReason());
    }

    @Test
    void shouldReturnPagedAuditTrail() {
        AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        AttendanceAuditTrailRepository attendanceAuditTrailRepository = mock(AttendanceAuditTrailRepository.class);
        AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository = mock(AttendanceIdempotencyKeyRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        SiteService siteService = mock(SiteService.class);

        AttendanceService service = buildService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            attendanceIdempotencyKeyRepository,
            employeeService,
            siteService
        );

        AttendanceRecord record = buildRecord(77L);
        AttendanceAuditTrail auditTrail = new AttendanceAuditTrail();
        ReflectionTestUtils.setField(auditTrail, "id", 700L);
        auditTrail.setAttendanceRecord(record);
        auditTrail.setAction(AttendanceAuditAction.CANCELLED);
        auditTrail.setActorSubject("admin.user");
        auditTrail.setActorRoles("ROLE_ADMIN");
        auditTrail.setReason("manual cancellation");
        auditTrail.setCreatedAt(Instant.parse("2026-04-08T10:10:00Z"));

        when(attendanceRecordRepository.existsById(77L)).thenReturn(true);
        when(attendanceAuditTrailRepository.findByAttendanceRecordId(anyLong(), any()))
            .thenReturn(new PageImpl<>(List.of(auditTrail), PageRequest.of(0, 20), 1));

        SecurityContextHolder.getContext().setAuthentication(buildAdminAuthentication());
        var page = service.getAttendanceAuditTrail(77L, 0, 20, "createdAt", "DESC");

        assertEquals(1, page.content().size());
        assertEquals(AttendanceAuditAction.CANCELLED, page.content().getFirst().action());
    }

    @Test
    void shouldRejectCorrectionForEmployeeRole() {
        AttendanceRecordRepository attendanceRecordRepository = mock(AttendanceRecordRepository.class);
        AttendanceAuditTrailRepository attendanceAuditTrailRepository = mock(AttendanceAuditTrailRepository.class);
        AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository = mock(AttendanceIdempotencyKeyRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        SiteService siteService = mock(SiteService.class);

        AttendanceService service = buildService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            attendanceIdempotencyKeyRepository,
            employeeService,
            siteService
        );

        SecurityContextHolder.getContext().setAuthentication(buildEmployeeAuthentication());

        AttendanceCorrectionRequest request = new AttendanceCorrectionRequest(
            OffsetDateTime.parse("2026-04-08T09:40:00+01:00"),
            AttendanceSource.MOBILE_APP,
            AttendanceStatus.UNVERIFIED,
            "note",
            "reason"
        );

        assertThrows(AccessDeniedException.class, () -> service.correctAttendance(1L, request));
    }

    private AttendanceService buildService(
        AttendanceRecordRepository attendanceRecordRepository,
        AttendanceAuditTrailRepository attendanceAuditTrailRepository,
        AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository,
        EmployeeService employeeService,
        SiteService siteService
    ) {
        AttendanceRulesProperties rules = new AttendanceRulesProperties();
        rules.setDuplicateGuardEnabled(false);
        rules.setEnforceDailyWindow(false);
        rules.setMaxFutureToleranceSeconds(120);
        rules.setMaxRetroactiveMinutes(1440);

        return new AttendanceService(
            attendanceRecordRepository,
            attendanceAuditTrailRepository,
            attendanceIdempotencyKeyRepository,
            employeeService,
            siteService,
            rules,
            Clock.fixed(Instant.parse("2026-04-08T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    private AttendanceRecord buildRecord(Long id) {
        Employee employee = new Employee();
        ReflectionTestUtils.setField(employee, "id", 99L);
        employee.setEmployeeCode("EMP-099");

        CompanySite site = new CompanySite();
        ReflectionTestUtils.setField(site, "id", 5L);
        site.setCode("SITE-005");
        site.setTimezone("Africa/Douala");

        AttendanceRecord record = new AttendanceRecord();
        ReflectionTestUtils.setField(record, "id", id);
        record.setEmployee(employee);
        record.setSite(site);
        record.setArrivalTime(Instant.parse("2026-04-08T08:30:00Z"));
        record.setRecordedAt(Instant.parse("2026-04-08T08:31:00Z"));
        record.setCheckInSource(AttendanceSource.MOBILE_APP);
        record.setStatus(AttendanceStatus.UNVERIFIED);
        record.setNotes("initial");
        return record;
    }

    private JwtAuthenticationToken buildAdminAuthentication() {
        Jwt jwt = new Jwt(
            "admin-token",
            Instant.parse("2026-04-08T09:00:00Z"),
            Instant.parse("2026-04-08T11:00:00Z"),
            Map.of("alg", "HS256"),
            Map.of("sub", "admin.user", "roles", List.of("ADMIN"))
        );
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private JwtAuthenticationToken buildEmployeeAuthentication() {
        Jwt jwt = new Jwt(
            "employee-token",
            Instant.parse("2026-04-08T09:00:00Z"),
            Instant.parse("2026-04-08T11:00:00Z"),
            Map.of("alg", "HS256"),
            Map.of("sub", "employee.user", "roles", List.of("EMPLOYEE"))
        );
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
    }
}
