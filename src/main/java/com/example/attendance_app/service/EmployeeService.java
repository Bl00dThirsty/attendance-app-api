package com.example.attendance_app.service;

import com.example.attendance_app.dto.employee.EmployeeCreateRequest;
import com.example.attendance_app.dto.employee.EmployeeResponse;
import com.example.attendance_app.entity.Department;
import com.example.attendance_app.entity.Employee;
import com.example.attendance_app.entity.EmployeeRole;
import com.example.attendance_app.entity.JobPosition;
import com.example.attendance_app.exception.BadRequestException;
import com.example.attendance_app.exception.ConflictException;
import com.example.attendance_app.exception.ResourceNotFoundException;
import com.example.attendance_app.repository.AttendanceRecordRepository;
import com.example.attendance_app.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;
    private final JobPositionService jobPositionService;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public EmployeeService(
        EmployeeRepository employeeRepository,
        DepartmentService departmentService,
        JobPositionService jobPositionService,
        AttendanceRecordRepository attendanceRecordRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.departmentService = departmentService;
        this.jobPositionService = jobPositionService;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        Employee employee = new Employee();
        String employeeCode = request.employeeCode().trim();
        String email = request.email().trim().toLowerCase();
        validateUniqueFields(employeeCode, email, null);

        applyEmployeeData(employee, request, employeeCode, email);
        employee.setRole(request.role() == null ? EmployeeRole.EMPLOYEE : request.role());
        employee.setActive(request.active() == null || request.active());

        return mapToResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse updateEmployee(Long id, EmployeeCreateRequest request) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));

        String employeeCode = request.employeeCode().trim();
        String email = request.email().trim().toLowerCase();
        validateUniqueFields(employeeCode, email, id);

        applyEmployeeData(employee, request, employeeCode, email);
        if (request.role() != null) {
            employee.setRole(request.role());
        }
        if (request.active() != null) {
            employee.setActive(request.active());
        }

        return mapToResponse(employeeRepository.save(employee));
    }

    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));

        if (attendanceRecordRepository.existsByEmployeeId(id)) {
            throw new ConflictException("Employee has attendance records and cannot be deleted");
        }

        employeeRepository.delete(employee);
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

    private void applyEmployeeData(Employee employee, EmployeeCreateRequest request, String employeeCode, String email) {
        validateDateConsistency(request);

        employee.setEmployeeCode(employeeCode);
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setPosition(resolvePosition(request.positionId()));
        employee.setDepartment(resolveDepartment(request.departmentId()));
        employee.setHireDate(request.hireDate());
        employee.setBirthDate(request.birthDate());
        employee.setBirthPlace(normalizeOptionalText(request.birthPlace()));
        employee.setContractType(request.contractType());
        employee.setEmployeeType(request.employeeType());
        employee.setMaritalStatus(request.maritalStatus());
        employee.setGender(request.gender());
        employee.setCityOfResidence(normalizeOptionalText(request.cityOfResidence()));
        employee.setDistrict(normalizeOptionalText(request.district()));
        employee.setNationality(normalizeOptionalText(request.nationality()));
        employee.setNationalIdNumber(normalizeOptionalText(request.nationalIdNumber()));
        employee.setPhoneNumber(normalizeOptionalText(request.phoneNumber()));
        employee.setAddress(normalizeOptionalText(request.address()));
        employee.setEmergencyContactName(normalizeOptionalText(request.emergencyContactName()));
        employee.setEmergencyContactPhone(normalizeOptionalText(request.emergencyContactPhone()));
        employee.setContractStartDate(request.contractStartDate());
        employee.setContractEndDate(request.contractEndDate());
    }

    private void validateDateConsistency(EmployeeCreateRequest request) {
        if (
            request.birthDate() != null &&
            request.hireDate() != null &&
            request.hireDate().isBefore(request.birthDate())
        ) {
            throw new BadRequestException("hireDate cannot be before birthDate");
        }

        if (
            request.contractStartDate() != null &&
            request.contractEndDate() != null &&
            request.contractEndDate().isBefore(request.contractStartDate())
        ) {
            throw new BadRequestException("contractEndDate cannot be before contractStartDate");
        }

        if (
            request.hireDate() != null &&
            request.contractStartDate() != null &&
            request.contractStartDate().isBefore(request.hireDate())
        ) {
            throw new BadRequestException("contractStartDate cannot be before hireDate");
        }
    }

    private void validateUniqueFields(String employeeCode, String email, Long employeeId) {
        boolean duplicateCode = employeeId == null
            ? employeeRepository.existsByEmployeeCodeIgnoreCase(employeeCode)
            : employeeRepository.existsByEmployeeCodeIgnoreCaseAndIdNot(employeeCode, employeeId);
        if (duplicateCode) {
            throw new ConflictException("Employee code already exists");
        }

        boolean duplicateEmail = employeeId == null
            ? employeeRepository.existsByEmailIgnoreCase(email)
            : employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, employeeId);
        if (duplicateEmail) {
            throw new ConflictException("Employee email already exists");
        }
    }

    private JobPosition resolvePosition(Long positionId) {
        if (positionId == null) {
            return null;
        }
        return jobPositionService.getPositionEntity(positionId);
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentService.getDepartmentEntity(departmentId);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private EmployeeResponse mapToResponse(Employee employee) {
        JobPosition position = employee.getPosition();
        Department department = employee.getDepartment();

        return new EmployeeResponse(
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getEmail(),
            position == null ? null : position.getId(),
            position == null ? null : position.getCode(),
            position == null ? null : position.getName(),
            department == null ? null : department.getId(),
            department == null ? null : department.getCode(),
            department == null ? null : department.getName(),
            employee.getHireDate(),
            employee.getBirthDate(),
            employee.getBirthPlace(),
            employee.getContractType(),
            employee.getEmployeeType(),
            employee.getMaritalStatus(),
            employee.getGender(),
            employee.getCityOfResidence(),
            employee.getDistrict(),
            employee.getNationality(),
            employee.getNationalIdNumber(),
            employee.getPhoneNumber(),
            employee.getAddress(),
            employee.getEmergencyContactName(),
            employee.getEmergencyContactPhone(),
            employee.getContractStartDate(),
            employee.getContractEndDate(),
            employee.getRole(),
            employee.isActive(),
            employee.getCreatedAt(),
            employee.getUpdatedAt()
        );
    }
}
