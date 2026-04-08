package com.example.attendance_app.dto.site;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SiteCreateRequest(
    @NotBlank(message = "code is required")
    @Size(max = 50, message = "code must not exceed 50 characters")
    String code,

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must not exceed 120 characters")
    String name,

    @NotBlank(message = "address is required")
    @Size(max = 255, message = "address must not exceed 255 characters")
    String address,

    @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "latitude must be <= 90")
    Double latitude,

    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "longitude must be <= 180")
    Double longitude,

    @Min(value = 1, message = "geofenceRadiusMeters must be at least 1")
    Integer geofenceRadiusMeters,

    @Size(max = 64, message = "timezone must not exceed 64 characters")
    String timezone,

    Boolean active
) {
}
