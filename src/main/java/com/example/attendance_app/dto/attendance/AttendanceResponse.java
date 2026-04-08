package com.example.attendance_app.dto.attendance;

import com.example.attendance_app.entity.AttendanceSource;
import com.example.attendance_app.entity.AttendanceStatus;

import java.time.LocalDateTime;

public record AttendanceResponse(
    Long id,
    Long employeeId,
    String employeeCode,
    Long siteId,
    String siteCode,
    LocalDateTime arrivalTime,
    AttendanceSource checkInSource,
    AttendanceStatus status,
    Double latitude,
    Double longitude,
    Double distanceMeters,
    Boolean withinSiteRange,
    String notes,
    LocalDateTime recordedAt
) {
}
