package com.example.attendance_app.service;

import com.example.attendance_app.dto.site.SiteCreateRequest;
import com.example.attendance_app.dto.site.SiteResponse;
import com.example.attendance_app.entity.CompanySite;
import com.example.attendance_app.exception.BadRequestException;
import com.example.attendance_app.exception.ConflictException;
import com.example.attendance_app.exception.ResourceNotFoundException;
import com.example.attendance_app.repository.CompanySiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SiteService {

    private final CompanySiteRepository companySiteRepository;

    public SiteService(CompanySiteRepository companySiteRepository) {
        this.companySiteRepository = companySiteRepository;
    }

    public SiteResponse createSite(SiteCreateRequest request) {
        if (companySiteRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ConflictException("Site code already exists");
        }

        boolean hasLatitude = request.latitude() != null;
        boolean hasLongitude = request.longitude() != null;
        if (hasLatitude != hasLongitude) {
            throw new BadRequestException("latitude and longitude must be provided together");
        }

        CompanySite site = new CompanySite();
        site.setCode(request.code().trim());
        site.setName(request.name().trim());
        site.setAddress(request.address().trim());
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        site.setGeofenceRadiusMeters(request.geofenceRadiusMeters() == null ? 100 : request.geofenceRadiusMeters());
        site.setActive(request.active() == null || request.active());

        return mapToResponse(companySiteRepository.save(site));
    }

    @Transactional(readOnly = true)
    public SiteResponse getSite(Long id) {
        CompanySite site = companySiteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + id));
        return mapToResponse(site);
    }

    @Transactional(readOnly = true)
    public List<SiteResponse> getSites() {
        return companySiteRepository.findAll().stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public CompanySite getSiteEntity(Long id) {
        return companySiteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + id));
    }

    private SiteResponse mapToResponse(CompanySite site) {
        return new SiteResponse(
            site.getId(),
            site.getCode(),
            site.getName(),
            site.getAddress(),
            site.getLatitude(),
            site.getLongitude(),
            site.getGeofenceRadiusMeters(),
            site.isActive(),
            site.getCreatedAt(),
            site.getUpdatedAt()
        );
    }
}
