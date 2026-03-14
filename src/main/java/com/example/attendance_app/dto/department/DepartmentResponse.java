package com.example.attendance_app.dto.department;

import java.time.LocalDateTime;

public record DepartmentResponse(
    Long id,
    String code,
    String name,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
