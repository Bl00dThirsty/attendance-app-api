package com.example.attendance_app.dto.attendance;

import com.example.attendance_app.entity.AttendanceSource;
import com.example.attendance_app.entity.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record AttendanceCorrectionRequest(
    OffsetDateTime arrivalTime,

    AttendanceSource checkInSource,

    AttendanceStatus status,

    @Size(max = 500, message = "notes must not exceed 500 characters")
    String notes,

    @NotBlank(message = "reason is required")
    @Size(max = 500, message = "reason must not exceed 500 characters")
    String reason
) {
}
