package com.example.attendance_app.controller;

import com.example.attendance_app.dto.position.JobPositionCreateRequest;
import com.example.attendance_app.dto.position.JobPositionResponse;
import com.example.attendance_app.service.JobPositionService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@Tag(name = "Positions", description = "Job position management endpoints")
public class JobPositionController {

    private final JobPositionService jobPositionService;

    public JobPositionController(JobPositionService jobPositionService) {
        this.jobPositionService = jobPositionService;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Create a new job position
    ----------------------------------------------------------------
    @parameter: JobPositionCreateRequest request
    @Returnvalue: JobPositionResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create position")
    public JobPositionResponse createPosition(@Valid @RequestBody JobPositionCreateRequest request) {
        return jobPositionService.createPosition(request);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Update a job position by id
    ----------------------------------------------------------------
    @parameter: Long id, JobPositionCreateRequest request
    @Returnvalue: JobPositionResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @PutMapping("/{id}")
    @Operation(summary = "Update position")
    public JobPositionResponse updatePosition(@PathVariable Long id, @Valid @RequestBody JobPositionCreateRequest request) {
        return jobPositionService.updatePosition(id, request);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Delete a job position by id
    ----------------------------------------------------------------
    @parameter: Long id
    @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete position")
    public void deletePosition(@PathVariable Long id) {
        jobPositionService.deletePosition(id);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Retrieve all job positions
    ----------------------------------------------------------------
    @parameter: -
    @Returnvalue: List<JobPositionResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @GetMapping
    @Operation(summary = "List positions")
    public List<JobPositionResponse> getPositions() {
        return jobPositionService.getPositions();
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Retrieve one job position by id
    ----------------------------------------------------------------
    @parameter: Long id
    @Returnvalue: JobPositionResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @GetMapping("/{id}")
    @Operation(summary = "Get position by ID")
    public JobPositionResponse getPosition(@PathVariable Long id) {
        return jobPositionService.getPosition(id);
    }
}
