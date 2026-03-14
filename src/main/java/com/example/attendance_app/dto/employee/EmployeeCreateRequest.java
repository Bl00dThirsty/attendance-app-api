package com.example.attendance_app.dto.employee;

import com.example.attendance_app.entity.EmployeeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeCreateRequest(
    @NotBlank(message = "employeeCode is required")
    @Size(max = 50, message = "employeeCode must not exceed 50 characters")
    String employeeCode,

    @NotBlank(message = "firstName is required")
    @Size(max = 100, message = "firstName must not exceed 100 characters")
    String firstName,

    @NotBlank(message = "lastName is required")
    @Size(max = 100, message = "lastName must not exceed 100 characters")
    String lastName,

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Size(max = 150, message = "email must not exceed 150 characters")
    String email,

    @Size(max = 120, message = "position must not exceed 120 characters")
    String position,

    Long departmentId,

    EmployeeRole role,

    Boolean active
) {
}
