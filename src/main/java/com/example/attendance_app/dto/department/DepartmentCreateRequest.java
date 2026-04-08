package com.example.attendance_app.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentCreateRequest(
    @NotBlank(message = "code is required")
    @Size(max = 50, message = "code must not exceed 50 characters")
    String code,

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must not exceed 120 characters")
    String name,

    Boolean active
) {
}
