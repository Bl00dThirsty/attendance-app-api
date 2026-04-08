package com.example.attendance_app.dto.attendance;

import com.example.attendance_app.entity.AttendanceSource;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AttendanceCheckInRequest(
    @NotNull(message = "employeeId is required")
    Long employeeId,

    @NotNull(message = "siteId is required")
    Long siteId,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime arrivalTime,

    AttendanceSource checkInSource,

    @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "latitude must be <= 90")
    Double latitude,

    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "longitude must be <= 180")
    Double longitude,

    @Size(max = 500, message = "notes must not exceed 500 characters")
    String notes
) {
}
