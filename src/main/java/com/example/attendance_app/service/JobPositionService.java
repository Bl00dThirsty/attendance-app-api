package com.example.attendance_app.service;

import com.example.attendance_app.dto.position.JobPositionCreateRequest;
import com.example.attendance_app.dto.position.JobPositionResponse;
import com.example.attendance_app.entity.JobPosition;
import com.example.attendance_app.exception.ConflictException;
import com.example.attendance_app.exception.ResourceNotFoundException;
import com.example.attendance_app.repository.EmployeeRepository;
import com.example.attendance_app.repository.JobPositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class JobPositionService {

    private final JobPositionRepository jobPositionRepository;
    private final EmployeeRepository employeeRepository;

    public JobPositionService(JobPositionRepository jobPositionRepository, EmployeeRepository employeeRepository) {
        this.jobPositionRepository = jobPositionRepository;
        this.employeeRepository = employeeRepository;
    }

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

    public void deletePosition(Long id) {
        JobPosition position = jobPositionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + id));

        if (employeeRepository.existsByPositionId(id)) {
            throw new ConflictException("Position is assigned to employees and cannot be deleted");
        }

        jobPositionRepository.delete(position);
    }

    @Transactional(readOnly = true)
    public JobPositionResponse getPosition(Long id) {
        JobPosition position = getPositionEntity(id);
        return mapToResponse(position);
    }

    @Transactional(readOnly = true)
    public List<JobPositionResponse> getPositions() {
        return jobPositionRepository.findAll().stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public JobPosition getPositionEntity(Long id) {
        return jobPositionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + id));
    }

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

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

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
