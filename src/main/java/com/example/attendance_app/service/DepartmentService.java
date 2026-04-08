package com.example.attendance_app.service;

import com.example.attendance_app.dto.department.DepartmentCreateRequest;
import com.example.attendance_app.dto.department.DepartmentResponse;
import com.example.attendance_app.entity.Department;
import com.example.attendance_app.exception.ConflictException;
import com.example.attendance_app.exception.ResourceNotFoundException;
import com.example.attendance_app.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Create a department after uniqueness checks
            ----------------------------------------------------------------
            @parameter: DepartmentCreateRequest request
            @Returnvalue: DepartmentResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        if (departmentRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ConflictException("Department code already exists");
        }
        if (departmentRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Department name already exists");
        }

        Department department = new Department();
        department.setCode(request.code().trim());
        department.setName(request.name().trim());
        department.setActive(request.active() == null || request.active());

        return mapToResponse(departmentRepository.save(department));
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve one department by id
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: DepartmentResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(Long id) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        return mapToResponse(department);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve all departments
            ----------------------------------------------------------------
            @parameter: -
            @Returnvalue: List<DepartmentResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments() {
        return departmentRepository.findAll().stream()
            .map(this::mapToResponse)
            .toList();
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve raw department entity by id
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: Department
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public Department getDepartmentEntity(Long id) {
        return departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Convert department entity to response DTO
            ----------------------------------------------------------------
            @parameter: Department department
            @Returnvalue: DepartmentResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private DepartmentResponse mapToResponse(Department department) {
        return new DepartmentResponse(
            department.getId(),
            department.getCode(),
            department.getName(),
            department.isActive(),
            department.getCreatedAt(),
            department.getUpdatedAt()
        );
    }
}
