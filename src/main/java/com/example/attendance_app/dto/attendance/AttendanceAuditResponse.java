package com.example.attendance_app.dto.attendance;

import com.example.attendance_app.entity.AttendanceAuditAction;

import java.time.Instant;

public record AttendanceAuditResponse(
    Long id,
    Long attendanceRecordId,
    AttendanceAuditAction action,
    String actorSubject,
    Long actorEmployeeId,
    String actorRoles,
    String reason,
    String details,
    Instant createdAt
) {
}
