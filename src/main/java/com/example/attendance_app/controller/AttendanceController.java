package com.example.attendance_app.controller;

import com.example.attendance_app.dto.attendance.AttendanceCheckInRequest;
import com.example.attendance_app.dto.attendance.AttendanceCancellationRequest;
import com.example.attendance_app.dto.attendance.AttendanceAuditResponse;
import com.example.attendance_app.dto.attendance.AttendanceCorrectionRequest;
import com.example.attendance_app.dto.attendance.AttendanceResponse;
import com.example.attendance_app.dto.common.PagedResponse;
import com.example.attendance_app.entity.AttendanceSource;
import com.example.attendance_app.entity.AttendanceStatus;
import com.example.attendance_app.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/attendance")
@Tag(name = "Attendance", description = "Attendance tracking endpoints")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Create a new attendance check-in
    ----------------------------------------------------------------
    @parameter: AttendanceCheckInRequest request
    @Returnvalue: AttendanceResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record employee check-in")
    public AttendanceResponse checkIn(
        @Valid @RequestBody AttendanceCheckInRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false)
        @Parameter(description = "Optional idempotency key to safely retry check-in requests")
        String idempotencyKey
    ) {
        return attendanceService.recordCheckIn(request, idempotencyKey);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Fetch one attendance record by identifier
    ----------------------------------------------------------------
    @parameter: Long id
    @Returnvalue: AttendanceResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @GetMapping("/{id}")
    @Operation(summary = "Get attendance record by ID")
    public AttendanceResponse getAttendanceById(@PathVariable Long id) {
        return attendanceService.getAttendanceById(id);
    }

    @PutMapping("/{id}/correction")
    @Operation(summary = "Correct an existing attendance record")
    public AttendanceResponse correctAttendance(
        @PathVariable Long id,
        @Valid @RequestBody AttendanceCorrectionRequest request
    ) {
        return attendanceService.correctAttendance(id, request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an attendance record")
    public AttendanceResponse cancelAttendance(
        @PathVariable Long id,
        @Valid @RequestBody AttendanceCancellationRequest request
    ) {
        return attendanceService.cancelAttendance(id, request);
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "List audit trail for one attendance record")
    public PagedResponse<AttendanceAuditResponse> getAttendanceAuditTrail(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        return attendanceService.getAttendanceAuditTrail(id, page, size, sortBy, sortDir);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Search attendance records with optional filters
    ----------------------------------------------------------------
    @parameter: employeeId, siteId, from, to
    @Returnvalue: List<AttendanceResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @GetMapping
    @Operation(summary = "Search attendance records")
    public PagedResponse<AttendanceResponse> searchAttendance(
        @RequestParam(required = false) Long employeeId,
        @RequestParam(required = false) Long siteId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = "From timestamp (ISO-8601 with offset, e.g. 2026-03-09T08:00:00+01:00)")
        OffsetDateTime from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = "To timestamp (ISO-8601 with offset, e.g. 2026-03-09T18:00:00+01:00)")
        OffsetDateTime to,
        @RequestParam(required = false) AttendanceStatus status,
        @RequestParam(required = false) AttendanceSource source,
        @RequestParam(required = false, name = "q")
        @Parameter(description = "Free-text search on employee code, site code, and notes")
        String query,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "arrivalTime") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        return attendanceService.searchAttendance(
            employeeId,
            siteId,
            from,
            to,
            status,
            source,
            query,
            page,
            size,
            sortBy,
            sortDir
        );
    }
}
