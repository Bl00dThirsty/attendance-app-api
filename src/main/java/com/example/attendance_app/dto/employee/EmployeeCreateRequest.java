package com.example.attendance_app.dto.employee;

import com.example.attendance_app.entity.ContractType;
import com.example.attendance_app.entity.EmployeeRole;
import com.example.attendance_app.entity.EmployeeType;
import com.example.attendance_app.entity.Gender;
import com.example.attendance_app.entity.MaritalStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

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

    Long positionId,

    Long departmentId,

    LocalDate hireDate,

    LocalDate birthDate,

    @Size(max = 150, message = "birthPlace must not exceed 150 characters")
    String birthPlace,

    ContractType contractType,

    EmployeeType employeeType,

    MaritalStatus maritalStatus,

    Gender gender,

    @Size(max = 120, message = "cityOfResidence must not exceed 120 characters")
    String cityOfResidence,

    @Size(max = 120, message = "district must not exceed 120 characters")
    String district,

    @Size(max = 80, message = "nationality must not exceed 80 characters")
    String nationality,

    @Size(max = 60, message = "nationalIdNumber must not exceed 60 characters")
    String nationalIdNumber,

    @Size(max = 30, message = "phoneNumber must not exceed 30 characters")
    String phoneNumber,

    @Size(max = 255, message = "address must not exceed 255 characters")
    String address,

    @Size(max = 150, message = "emergencyContactName must not exceed 150 characters")
    String emergencyContactName,

    @Size(max = 30, message = "emergencyContactPhone must not exceed 30 characters")
    String emergencyContactPhone,

    LocalDate contractStartDate,

    LocalDate contractEndDate,

    EmployeeRole role,

    Boolean active
) {
}
