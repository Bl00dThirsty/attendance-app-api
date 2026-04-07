package com.example.attendance_app.controller;

import com.example.attendance_app.dto.site.SiteCreateRequest;
import com.example.attendance_app.dto.site.SiteResponse;
import com.example.attendance_app.service.SiteService;
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
@RequestMapping("/api/sites")
@Tag(name = "Sites", description = "Company site management endpoints")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Create a company site
    ----------------------------------------------------------------
    @parameter: SiteCreateRequest request
    @Returnvalue: SiteResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create company site")
    public SiteResponse createSite(@Valid @RequestBody SiteCreateRequest request) {
        return siteService.createSite(request);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Update a company site by id
    ----------------------------------------------------------------
    @parameter: Long id, SiteCreateRequest request
    @Returnvalue: SiteResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @PutMapping("/{id}")
    @Operation(summary = "Update company site")
    public SiteResponse updateSite(@PathVariable Long id, @Valid @RequestBody SiteCreateRequest request) {
        return siteService.updateSite(id, request);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Delete a company site by id
    ----------------------------------------------------------------
    @parameter: Long id
    @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete company site")
    public void deleteSite(@PathVariable Long id) {
        siteService.deleteSite(id);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Retrieve all company sites
    ----------------------------------------------------------------
    @parameter: -
    @Returnvalue: List<SiteResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @GetMapping
    @Operation(summary = "List company sites")
    public List<SiteResponse> getSites() {
        return siteService.getSites();
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Author:    John MANGA | Digit-Tech-Innov solutions and services
    @Creation:  14.03.2026
    ----------------------------------------------------------------
    @Function Description: Retrieve one company site by id
    ----------------------------------------------------------------
    @parameter: Long id
    @Returnvalue: SiteResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @GetMapping("/{id}")
    @Operation(summary = "Get site by ID")
    public SiteResponse getSite(@PathVariable Long id) {
        return siteService.getSite(id);
    }
}
