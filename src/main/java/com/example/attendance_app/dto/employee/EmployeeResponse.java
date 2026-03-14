package com.example.attendance_app.dto.employee;

import com.example.attendance_app.entity.EmployeeRole;

import java.time.LocalDateTime;

public record EmployeeResponse(
    Long id,
    String employeeCode,
    String firstName,
    String lastName,
    String email,
    EmployeeRole role,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
