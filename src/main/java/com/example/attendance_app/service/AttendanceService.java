package com.example.attendance_app.service;

import com.example.attendance_app.dto.attendance.AttendanceCheckInRequest;
import com.example.attendance_app.dto.attendance.AttendanceResponse;
import com.example.attendance_app.entity.AttendanceRecord;
import com.example.attendance_app.entity.AttendanceSource;
import com.example.attendance_app.entity.AttendanceStatus;
import com.example.attendance_app.entity.CompanySite;
import com.example.attendance_app.entity.Employee;
import com.example.attendance_app.exception.BadRequestException;
import com.example.attendance_app.exception.ResourceNotFoundException;
import com.example.attendance_app.repository.AttendanceRecordRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AttendanceService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeService employeeService;
    private final SiteService siteService;

    public AttendanceService(
        AttendanceRecordRepository attendanceRecordRepository,
        EmployeeService employeeService,
        SiteService siteService
    ) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.employeeService = employeeService;
        this.siteService = siteService;
    }

    public AttendanceResponse recordCheckIn(AttendanceCheckInRequest request) {
        boolean hasLatitude = request.latitude() != null;
        boolean hasLongitude = request.longitude() != null;
        if (hasLatitude != hasLongitude) {
            throw new BadRequestException("latitude and longitude must be provided together");
        }

        Employee employee = employeeService.getEmployeeEntity(request.employeeId());
        if (!employee.isActive()) {
            throw new BadRequestException("Employee is inactive and cannot check in");
        }

        CompanySite site = siteService.getSiteEntity(request.siteId());
        if (!site.isActive()) {
            throw new BadRequestException("Site is inactive and cannot accept check-ins");
        }

        LocalDateTime arrivalTime = request.arrivalTime() == null ? LocalDateTime.now() : request.arrivalTime();
        AttendanceSource source = request.checkInSource() == null ? AttendanceSource.MOBILE_APP : request.checkInSource();

        AttendanceStatus status = AttendanceStatus.UNVERIFIED;
        Double distanceMeters = null;
        if (
            hasLatitude &&
            site.getLatitude() != null &&
            site.getLongitude() != null &&
            site.getGeofenceRadiusMeters() != null
        ) {
            distanceMeters = calculateDistanceMeters(
                site.getLatitude(),
                site.getLongitude(),
                request.latitude(),
                request.longitude()
            );
            status = distanceMeters <= site.getGeofenceRadiusMeters()
                ? AttendanceStatus.ON_SITE
                : AttendanceStatus.OFF_SITE;
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setEmployee(employee);
        record.setSite(site);
        record.setArrivalTime(arrivalTime);
        record.setCheckInSource(source);
        record.setStatus(status);
        record.setLatitude(request.latitude());
        record.setLongitude(request.longitude());
        record.setDistanceMeters(distanceMeters);
        record.setNotes(request.notes() == null ? null : request.notes().trim());

        return mapToResponse(attendanceRecordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceById(Long id) {
        AttendanceRecord record = attendanceRecordRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id));
        return mapToResponse(record);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> searchAttendance(
        Long employeeId,
        Long siteId,
        LocalDateTime from,
        LocalDateTime to
    ) {
        Specification<AttendanceRecord> specification = (root, query, cb) -> cb.conjunction();

        if (employeeId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }
        if (siteId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("site").get("id"), siteId));
        }
        if (from != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("arrivalTime"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("arrivalTime"), to));
        }

        return attendanceRecordRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "arrivalTime")).stream()
            .map(this::mapToResponse)
            .toList();
    }

    private AttendanceResponse mapToResponse(AttendanceRecord record) {
        Boolean withinSiteRange = null;
        if (record.getStatus() == AttendanceStatus.ON_SITE) {
            withinSiteRange = true;
        } else if (record.getStatus() == AttendanceStatus.OFF_SITE) {
            withinSiteRange = false;
        }

        return new AttendanceResponse(
            record.getId(),
            record.getEmployee().getId(),
            record.getEmployee().getEmployeeCode(),
            record.getSite().getId(),
            record.getSite().getCode(),
            record.getArrivalTime(),
            record.getCheckInSource(),
            record.getStatus(),
            record.getLatitude(),
            record.getLongitude(),
            record.getDistanceMeters(),
            withinSiteRange,
            record.getNotes(),
            record.getRecordedAt()
        );
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
