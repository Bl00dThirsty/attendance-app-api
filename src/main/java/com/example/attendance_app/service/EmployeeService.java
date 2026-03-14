package com.example.attendance_app.service;

import com.example.attendance_app.dto.employee.EmployeeCreateRequest;
import com.example.attendance_app.dto.employee.EmployeeResponse;
import com.example.attendance_app.entity.Department;
import com.example.attendance_app.entity.Employee;
import com.example.attendance_app.entity.EmployeeRole;
import com.example.attendance_app.exception.ConflictException;
import com.example.attendance_app.exception.ResourceNotFoundException;
import com.example.attendance_app.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentService departmentService) {
        this.employeeRepository = employeeRepository;
        this.departmentService = departmentService;
    }

    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        if (employeeRepository.existsByEmployeeCodeIgnoreCase(request.employeeCode())) {
            throw new ConflictException("Employee code already exists");
        }
        if (employeeRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Employee email already exists");
        }

        Employee employee = new Employee();
        employee.setEmployeeCode(request.employeeCode().trim());
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(request.email().trim().toLowerCase());
        if (request.position() != null) {
            String trimmedPosition = request.position().trim();
            employee.setPosition(trimmedPosition.isEmpty() ? null : trimmedPosition);
        }
        if (request.departmentId() != null) {
            Department department = departmentService.getDepartmentEntity(request.departmentId());
            employee.setDepartment(department);
        }
        employee.setRole(request.role() == null ? EmployeeRole.EMPLOYEE : request.role());
        employee.setActive(request.active() == null || request.active());

        return mapToResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
        return mapToResponse(employee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployees() {
        return employeeRepository.findAll().stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeEntity(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private EmployeeResponse mapToResponse(Employee employee) {
        Department department = employee.getDepartment();
        return new EmployeeResponse(
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getEmail(),
            employee.getPosition(),
            department == null ? null : department.getId(),
            department == null ? null : department.getCode(),
            department == null ? null : department.getName(),
            employee.getRole(),
            employee.isActive(),
            employee.getCreatedAt(),
            employee.getUpdatedAt()
        );
    }
}
