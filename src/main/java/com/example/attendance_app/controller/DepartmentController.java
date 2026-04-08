package com.example.attendance_app.controller;

import com.example.attendance_app.dto.common.PagedResponse;
import com.example.attendance_app.dto.department.DepartmentCreateRequest;
import com.example.attendance_app.dto.department.DepartmentResponse;
import com.example.attendance_app.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Departments", description = "Department management endpoints")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Create a new department
    ---------------------------------------------------------------
    @parameter: DepartmentCreateRequest request
    @Returnvalue: DepartmentResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create department")
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        return departmentService.createDepartment(request);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Function Description: Update an existing department by id
    ----------------------------------------------------------------
    @parameter: Long id, DepartmentCreateRequest request
    @Returnvalue: DepartmentResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @PutMapping("/{id}")
    @Operation(summary = "Update department")
    public DepartmentResponse updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentCreateRequest request) {
        return departmentService.updateDepartment(id, request);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Function Description: Delete an existing department by id
    ----------------------------------------------------------------
    @parameter: Long id
    @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete department")
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Retrieve all departments
    ----------------------------------------------------------------
    @parameter: -
    @Returnvalue: List<DepartmentResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @GetMapping
    @Operation(summary = "List departments")
    public PagedResponse<DepartmentResponse> getDepartments(
        @RequestParam(required = false, name = "q")
        @Parameter(description = "Free-text search on department code and name")
        String query,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        return departmentService.getDepartments(query, active, page, size, sortBy, sortDir);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Retrieve one department by id
    ----------------------------------------------------------------
    @parameter: Long id
    @Returnvalue: DepartmentResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    public DepartmentResponse getDepartment(@PathVariable Long id) {
        return departmentService.getDepartment(id);
    }
}
