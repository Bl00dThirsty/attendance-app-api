package com.example.attendance_app.controller;

import com.example.attendance_app.dto.site.SiteCreateRequest;
import com.example.attendance_app.dto.site.SiteResponse;
import com.example.attendance_app.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create company site")
    public SiteResponse createSite(@Valid @RequestBody SiteCreateRequest request) {
        return siteService.createSite(request);
    }

    @GetMapping
    @Operation(summary = "List company sites")
    public List<SiteResponse> getSites() {
        return siteService.getSites();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get site by ID")
    public SiteResponse getSite(@PathVariable Long id) {
        return siteService.getSite(id);
    }
}
