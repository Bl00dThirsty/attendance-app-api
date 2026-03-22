package com.example.attendance_app.dto.position;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobPositionCreateRequest(
    @NotBlank(message = "code is required")
    @Size(max = 50, message = "code must not exceed 50 characters")
    String code,

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must not exceed 120 characters")
    String name,

    @Size(max = 500, message = "description must not exceed 500 characters")
    String description,

    Boolean active
) {
}
