package com.example.attendance_app.controller;

import com.example.attendance_app.dto.common.PagedResponse;
import com.example.attendance_app.dto.employee.EmployeeCreateRequest;
import com.example.attendance_app.dto.employee.EmployeeResponse;
import com.example.attendance_app.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employee management endpoints")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Create a new employee
    ----------------------------------------------------------------
    @parameter: EmployeeCreateRequest request
    @Returnvalue: EmployeeResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create employee")
    public EmployeeResponse createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.createEmployee(request);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Update employee data by id
    ----------------------------------------------------------------
    @parameter: Long id, EmployeeCreateRequest request
    @Returnvalue: EmployeeResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @PutMapping("/{id}")
    @Operation(summary = "Update employee")
    public EmployeeResponse updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.updateEmployee(id, request);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Delete an employee by id
    ----------------------------------------------------------------
    @parameter: Long id
    @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete employee")
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Retrieve all employees
    ----------------------------------------------------------------
    @parameter: -
    @Returnvalue: List<EmployeeResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @GetMapping
    @Operation(summary = "List employees")
    public PagedResponse<EmployeeResponse> getEmployees(
        @RequestParam(required = false, name = "q")
        @Parameter(description = "Free-text search on employee code, first name, last name, and email")
        String query,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) Long positionId,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        return employeeService.getEmployees(query, departmentId, positionId, active, page, size, sortBy, sortDir);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Retrieve one employee by id
    ----------------------------------------------------------------
    @parameter: Long id
    @Returnvalue: EmployeeResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID")
    public EmployeeResponse getEmployee(@PathVariable Long id) {
        return employeeService.getEmployee(id);
    }
}
