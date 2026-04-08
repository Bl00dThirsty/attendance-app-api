package com.example.attendance_app.service;

import com.example.attendance_app.dto.common.PagedResponse;
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
import com.example.attendance_app.service.support.PageQuerySupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class EmployeeService {
    private static final Set<String> EMPLOYEE_SORT_FIELDS = Set.of(
        "id",
        "employeeCode",
        "firstName",
        "lastName",
        "email",
        "hireDate",
        "createdAt",
        "updatedAt",
        "active",
        "role"
    );

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

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Create a new employee with validations
            ----------------------------------------------------------------
            @parameter: EmployeeCreateRequest request
            @Returnvalue: EmployeeResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
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

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Update employee by id with business checks
            ----------------------------------------------------------------
            @parameter: Long id, EmployeeCreateRequest request
            @Returnvalue: EmployeeResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
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

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Delete employee if no attendance references
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));

        if (attendanceRecordRepository.existsByEmployeeId(id)) {
            throw new ConflictException("Employee has attendance records and cannot be deleted");
        }

        employeeRepository.delete(employee);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve one employee by id
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: EmployeeResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
        return mapToResponse(employee);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve all employees
            ----------------------------------------------------------------
            @parameter: query, departmentId, positionId, active, page, size, sortBy, sortDir
            @Returnvalue: PagedResponse<EmployeeResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> getEmployees(
        String query,
        Long departmentId,
        Long positionId,
        Boolean active,
        int page,
        int size,
        String sortBy,
        String sortDir
    ) {
        Pageable pageable = PageQuerySupport.buildPageable(
            page,
            size,
            sortBy,
            sortDir,
            EMPLOYEE_SORT_FIELDS,
            "createdAt",
            Sort.Direction.DESC
        );

        Specification<Employee> specification = (root, criteriaQuery, cb) -> cb.conjunction();
        if (query != null && !query.isBlank()) {
            String keyword = "%" + query.trim().toLowerCase() + "%";
            specification = specification.and((root, criteriaQuery, cb) -> cb.or(
                cb.like(cb.lower(root.get("employeeCode")), keyword),
                cb.like(cb.lower(root.get("firstName")), keyword),
                cb.like(cb.lower(root.get("lastName")), keyword),
                cb.like(cb.lower(root.get("email")), keyword)
            ));
        }
        if (departmentId != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (positionId != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.equal(root.get("position").get("id"), positionId));
        }
        if (active != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.equal(root.get("active"), active));
        }

        Page<EmployeeResponse> result = employeeRepository.findAll(specification, pageable)
            .map(this::mapToResponse);
        return PagedResponse.from(result);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve raw employee entity by id
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: Employee
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public Employee getEmployeeEntity(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Resolve employee id from external identity identifiers
            ----------------------------------------------------------------
            @parameter: String identifier (employeeCode or email)
            @Returnvalue: Optional<Long>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public Optional<Long> resolveEmployeeIdByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String trimmedIdentifier = identifier.trim();
        Optional<Long> byEmployeeCode = employeeRepository.findByEmployeeCodeIgnoreCase(trimmedIdentifier)
            .map(Employee::getId);
        if (byEmployeeCode.isPresent()) {
            return byEmployeeCode;
        }

        String normalizedEmail = trimmedIdentifier.toLowerCase();
        return employeeRepository.findByEmailIgnoreCase(normalizedEmail)
            .map(Employee::getId);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Apply normalized request data to employee entity
            ----------------------------------------------------------------
            @parameter: employee, request, employeeCode, email
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private void applyEmployeeData(Employee employee, EmployeeCreateRequest request, String employeeCode, String email) {
        // Validate date consistency before assigning values.
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

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Validate chronology between HR-related dates
            ----------------------------------------------------------------
            @parameter: EmployeeCreateRequest request
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
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

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Ensure employee code and email remain unique
            ----------------------------------------------------------------
            @parameter: employeeCode, email, employeeId
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
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

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Resolve optional job position reference
            ----------------------------------------------------------------
            @parameter: Long positionId
            @Returnvalue: JobPosition
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private JobPosition resolvePosition(Long positionId) {
        if (positionId == null) {
            return null;
        }
        return jobPositionService.getPositionEntity(positionId);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Resolve optional department reference
            ----------------------------------------------------------------
            @parameter: Long departmentId
            @Returnvalue: Department
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentService.getDepartmentEntity(departmentId);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Trim optional text and convert blanks to null
            ----------------------------------------------------------------
            @parameter: String value
            @Returnvalue: String
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Convert employee entity to response DTO
            ----------------------------------------------------------------
            @parameter: Employee employee
            @Returnvalue: EmployeeResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
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
