package com.example.attendance_app.service;

import com.example.attendance_app.dto.site.SiteCreateRequest;
import com.example.attendance_app.dto.site.SiteResponse;
import com.example.attendance_app.entity.CompanySite;
import com.example.attendance_app.exception.BadRequestException;
import com.example.attendance_app.exception.ConflictException;
import com.example.attendance_app.exception.ResourceNotFoundException;
import com.example.attendance_app.repository.AttendanceRecordRepository;
import com.example.attendance_app.repository.CompanySiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SiteService {

    private final CompanySiteRepository companySiteRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public SiteService(
        CompanySiteRepository companySiteRepository,
        AttendanceRecordRepository attendanceRecordRepository
    ) {
        this.companySiteRepository = companySiteRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Create a company site with coordinate checks
            ----------------------------------------------------------------
            @parameter: SiteCreateRequest request
            @Returnvalue: SiteResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public SiteResponse createSite(SiteCreateRequest request) {
        String code = request.code().trim();
        validateUniqueCode(code, null);
        validateCoordinates(request.latitude(), request.longitude());

        CompanySite site = new CompanySite();
        site.setCode(code);
        site.setName(request.name().trim());
        site.setAddress(request.address().trim());
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        // Use a default geofence when client does not provide one.
        site.setGeofenceRadiusMeters(request.geofenceRadiusMeters() == null ? 100 : request.geofenceRadiusMeters());
        site.setActive(request.active() == null || request.active());

        return mapToResponse(companySiteRepository.save(site));
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Update a site by id and apply validations
            ----------------------------------------------------------------
            @parameter: Long id, SiteCreateRequest request
            @Returnvalue: SiteResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public SiteResponse updateSite(Long id, SiteCreateRequest request) {
        CompanySite site = companySiteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + id));

        String code = request.code().trim();
        validateUniqueCode(code, id);
        validateCoordinates(request.latitude(), request.longitude());

        site.setCode(code);
        site.setName(request.name().trim());
        site.setAddress(request.address().trim());
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        if (request.geofenceRadiusMeters() != null) {
            site.setGeofenceRadiusMeters(request.geofenceRadiusMeters());
        }
        if (request.active() != null) {
            site.setActive(request.active());
        }

        return mapToResponse(companySiteRepository.save(site));
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Delete a site if no attendance depends on it
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public void deleteSite(Long id) {
        CompanySite site = companySiteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + id));

        if (attendanceRecordRepository.existsBySiteId(id)) {
            throw new ConflictException("Site has attendance records and cannot be deleted");
        }

        companySiteRepository.delete(site);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve one site by id
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: SiteResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public SiteResponse getSite(Long id) {
        CompanySite site = companySiteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + id));
        return mapToResponse(site);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve all company sites
            ----------------------------------------------------------------
            @parameter: -
            @Returnvalue: List<SiteResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public List<SiteResponse> getSites() {
        return companySiteRepository.findAll().stream()
            .map(this::mapToResponse)
            .toList();
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve raw site entity by id
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: CompanySite
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public CompanySite getSiteEntity(Long id) {
        return companySiteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + id));
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Enforce uniqueness of site code
            ----------------------------------------------------------------
            @parameter: code, siteId
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private void validateUniqueCode(String code, Long siteId) {
        boolean duplicateCode = siteId == null
            ? companySiteRepository.existsByCodeIgnoreCase(code)
            : companySiteRepository.existsByCodeIgnoreCaseAndIdNot(code, siteId);
        if (duplicateCode) {
            throw new ConflictException("Site code already exists");
        }
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Validate latitude/longitude completeness
            ----------------------------------------------------------------
            @parameter: latitude, longitude
            @Returnvalue: -
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private void validateCoordinates(Double latitude, Double longitude) {
        boolean hasLatitude = latitude != null;
        boolean hasLongitude = longitude != null;
        if (hasLatitude != hasLongitude) {
            throw new BadRequestException("latitude and longitude must be provided together");
        }
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Convert site entity to response DTO
            ----------------------------------------------------------------
            @parameter: CompanySite site
            @Returnvalue: SiteResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
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
