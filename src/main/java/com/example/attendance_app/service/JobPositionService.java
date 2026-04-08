package com.example.attendance_app.service;

import com.example.attendance_app.dto.common.PagedResponse;
import com.example.attendance_app.dto.position.JobPositionCreateRequest;
import com.example.attendance_app.dto.position.JobPositionResponse;
import com.example.attendance_app.entity.JobPosition;
import com.example.attendance_app.exception.ConflictException;
import com.example.attendance_app.exception.ResourceNotFoundException;
import com.example.attendance_app.repository.EmployeeRepository;
import com.example.attendance_app.repository.JobPositionRepository;
import com.example.attendance_app.service.support.PageQuerySupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class JobPositionService {
    private static final Set<String> POSITION_SORT_FIELDS = Set.of(
        "id",
        "code",
        "name",
        "createdAt",
        "updatedAt",
        "active"
    );

    private final JobPositionRepository jobPositionRepository;
    private final EmployeeRepository employeeRepository;

    public JobPositionService(JobPositionRepository jobPositionRepository, EmployeeRepository employeeRepository) {
        this.jobPositionRepository = jobPositionRepository;
        this.employeeRepository = employeeRepository;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Create a job position after uniqueness checks
            ----------------------------------------------------------------
            @parameter: JobPositionCreateRequest request
            @Returnvalue: JobPositionResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public JobPositionResponse createPosition(JobPositionCreateRequest request) {
        String code = request.code().trim();
        String name = request.name().trim();
        validateUniqueness(code, name, null);

        JobPosition position = new JobPosition();
        position.setCode(code);
        position.setName(name);
        position.setDescription(normalizeOptionalText(request.description()));
        position.setActive(request.active() == null || request.active());

        return mapToResponse(jobPositionRepository.save(position));
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Update a job position by id
            ----------------------------------------------------------------
            @parameter: Long id, JobPositionCreateRequest request
            @Returnvalue: JobPositionResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public JobPositionResponse updatePosition(Long id, JobPositionCreateRequest request) {
        JobPosition position = jobPositionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + id));

        String code = request.code().trim();
        String name = request.name().trim();
        validateUniqueness(code, name, id);

        position.setCode(code);
        position.setName(name);
        position.setDescription(normalizeOptionalText(request.description()));
        if (request.active() != null) {
            position.setActive(request.active());
        }

        return mapToResponse(jobPositionRepository.save(position));
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Delete a position if not linked to employees
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public void deletePosition(Long id) {
        JobPosition position = jobPositionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + id));

        if (employeeRepository.existsByPositionId(id)) {
            throw new ConflictException("Position is assigned to employees and cannot be deleted");
        }

        jobPositionRepository.delete(position);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve one position by id
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: JobPositionResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public JobPositionResponse getPosition(Long id) {
        JobPosition position = getPositionEntity(id);
        return mapToResponse(position);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve all positions
            ----------------------------------------------------------------
            @parameter: query, active, page, size, sortBy, sortDir
            @Returnvalue: PagedResponse<JobPositionResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public PagedResponse<JobPositionResponse> getPositions(
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
            POSITION_SORT_FIELDS,
            "createdAt",
            Sort.Direction.DESC
        );

        Specification<JobPosition> specification = (root, criteriaQuery, cb) -> cb.conjunction();
        if (query != null && !query.isBlank()) {
            String keyword = "%" + query.trim().toLowerCase() + "%";
            specification = specification.and((root, criteriaQuery, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), keyword),
                cb.like(cb.lower(root.get("name")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("description"), "")), keyword)
            ));
        }
        if (active != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.equal(root.get("active"), active));
        }

        Page<JobPositionResponse> result = jobPositionRepository.findAll(specification, pageable)
            .map(this::mapToResponse);
        return PagedResponse.from(result);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve raw position entity by id
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: JobPosition
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public JobPosition getPositionEntity(Long id) {
        return jobPositionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + id));
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Validate unique code and unique name
            ----------------------------------------------------------------
            @parameter: code, name, id
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private void validateUniqueness(String code, String name, Long id) {
        boolean codeExists = id == null
            ? jobPositionRepository.existsByCodeIgnoreCase(code)
            : jobPositionRepository.existsByCodeIgnoreCaseAndIdNot(code, id);
        if (codeExists) {
            throw new ConflictException("Position code already exists");
        }

        boolean nameExists = id == null
            ? jobPositionRepository.existsByNameIgnoreCase(name)
            : jobPositionRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (nameExists) {
            throw new ConflictException("Position name already exists");
        }
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Normalize optional textual input
            ----------------------------------------------------------------
            @parameter: String value
            @Returnvalue: String
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Convert position entity to response DTO
            ----------------------------------------------------------------
            @parameter: JobPosition position
            @Returnvalue: JobPositionResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private JobPositionResponse mapToResponse(JobPosition position) {
        return new JobPositionResponse(
            position.getId(),
            position.getCode(),
            position.getName(),
            position.getDescription(),
            position.isActive(),
            position.getCreatedAt(),
            position.getUpdatedAt()
        );
    }
}
