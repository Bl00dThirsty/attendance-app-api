package com.example.attendance_app.dto.site;

import java.time.LocalDateTime;

public record SiteResponse(
    Long id,
    String code,
    String name,
    String address,
    Double latitude,
    Double longitude,
    Integer geofenceRadiusMeters,
    String timezone,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
