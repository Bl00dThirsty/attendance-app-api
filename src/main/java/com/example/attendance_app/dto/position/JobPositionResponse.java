package com.example.attendance_app.dto.position;

import java.time.LocalDateTime;

public record JobPositionResponse(
    Long id,
    String code,
    String name,
    String description,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
