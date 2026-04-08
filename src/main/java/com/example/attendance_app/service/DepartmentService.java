package com.example.attendance_app.service;

import com.example.attendance_app.dto.common.PagedResponse;
import com.example.attendance_app.dto.department.DepartmentCreateRequest;
import com.example.attendance_app.dto.department.DepartmentResponse;
import com.example.attendance_app.entity.Department;
import com.example.attendance_app.exception.ConflictException;
import com.example.attendance_app.exception.ResourceNotFoundException;
import com.example.attendance_app.repository.DepartmentRepository;
import com.example.attendance_app.service.support.PageQuerySupport;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class DepartmentService {
    private static final Set<String> DEPARTMENT_SORT_FIELDS = Set.of(
        "id",
        "code",
        "name",
        "createdAt",
        "updatedAt",
        "active"
    );

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
            @Function Description: Update a department with uniqueness checks
            ----------------------------------------------------------------
            @parameter: Long id, DepartmentCreateRequest request
            @Returnvalue: DepartmentResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public DepartmentResponse updateDepartment(Long id, DepartmentCreateRequest request) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));

        String code = request.code().trim();
        String name = request.name().trim();
        validateUniqueFields(code, name, department.getCode(), department.getName());

        department.setCode(code);
        department.setName(name);
        if (request.active() != null) {
            department.setActive(request.active());
        }

        return mapToResponse(departmentRepository.save(department));
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Delete a department if no FK references exist
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        try {
            departmentRepository.delete(department);
            departmentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Department is referenced and cannot be deleted");
        }
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
            @parameter: query, active, page, size, sortBy, sortDir
            @Returnvalue: PagedResponse<DepartmentResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public PagedResponse<DepartmentResponse> getDepartments(
        String query,
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
            DEPARTMENT_SORT_FIELDS,
            "createdAt",
            Sort.Direction.DESC
        );

        Specification<Department> specification = (root, criteriaQuery, cb) -> cb.conjunction();
        if (query != null && !query.isBlank()) {
            String keyword = "%" + query.trim().toLowerCase() + "%";
            specification = specification.and((root, criteriaQuery, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), keyword),
                cb.like(cb.lower(root.get("name")), keyword)
            ));
        }
        if (active != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.equal(root.get("active"), active));
        }

        Page<DepartmentResponse> result = departmentRepository.findAll(specification, pageable)
            .map(this::mapToResponse);
        return PagedResponse.from(result);
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
            @Function Description: Ensure department code and name remain unique
            ----------------------------------------------------------------
            @parameter: code, name, currentCode, currentName
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private void validateUniqueFields(String code, String name, String currentCode, String currentName) {
        if (!currentCode.equalsIgnoreCase(code) && departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Department code already exists");
        }
        if (!currentName.equalsIgnoreCase(name) && departmentRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Department name already exists");
        }
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
