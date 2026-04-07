package com.example.attendance_app.controller;

import com.example.attendance_app.dto.attendance.AttendanceCheckInRequest;
import com.example.attendance_app.dto.attendance.AttendanceResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

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
    public AttendanceResponse checkIn(@Valid @RequestBody AttendanceCheckInRequest request) {
        return attendanceService.recordCheckIn(request);
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
    public List<AttendanceResponse> searchAttendance(
        @RequestParam(required = false) Long employeeId,
        @RequestParam(required = false) Long siteId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = "From timestamp (ISO-8601, e.g. 2026-03-09T08:00:00)")
        LocalDateTime from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(description = "To timestamp (ISO-8601, e.g. 2026-03-09T18:00:00)")
        LocalDateTime to
    ) {
        return attendanceService.searchAttendance(employeeId, siteId, from, to);
    }
}
