package com.example.attendance_app.dto.employee;

import com.example.attendance_app.entity.ContractType;
import com.example.attendance_app.entity.EmployeeRole;
import com.example.attendance_app.entity.EmployeeType;
import com.example.attendance_app.entity.Gender;
import com.example.attendance_app.entity.MaritalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
    Long id,
    String employeeCode,
    String firstName,
    String lastName,
    String email,
    Long positionId,
    String positionCode,
    String positionName,
    Long departmentId,
    String departmentCode,
    String departmentName,
    LocalDate hireDate,
    LocalDate birthDate,
    String birthPlace,
    ContractType contractType,
    EmployeeType employeeType,
    MaritalStatus maritalStatus,
    Gender gender,
    String cityOfResidence,
    String district,
    String nationality,
    String nationalIdNumber,
    String phoneNumber,
    String address,
    String emergencyContactName,
    String emergencyContactPhone,
    LocalDate contractStartDate,
    LocalDate contractEndDate,
    EmployeeRole role,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
