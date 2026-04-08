package com.example.attendance_app.service;

import com.example.attendance_app.config.AttendanceRulesProperties;
import com.example.attendance_app.dto.attendance.AttendanceAuditResponse;
import com.example.attendance_app.dto.attendance.AttendanceCheckInRequest;
import com.example.attendance_app.dto.attendance.AttendanceCancellationRequest;
import com.example.attendance_app.dto.attendance.AttendanceCorrectionRequest;
import com.example.attendance_app.dto.attendance.AttendanceResponse;
import com.example.attendance_app.dto.common.PagedResponse;
import com.example.attendance_app.entity.AttendanceAuditAction;
import com.example.attendance_app.entity.AttendanceAuditTrail;
import com.example.attendance_app.entity.AttendanceIdempotencyKey;
import com.example.attendance_app.entity.AttendanceRecord;
import com.example.attendance_app.entity.AttendanceSource;
import com.example.attendance_app.entity.AttendanceStatus;
import com.example.attendance_app.entity.CompanySite;
import com.example.attendance_app.entity.Employee;
import com.example.attendance_app.exception.BadRequestException;
import com.example.attendance_app.exception.ConflictException;
import com.example.attendance_app.exception.ResourceNotFoundException;
import com.example.attendance_app.repository.AttendanceAuditTrailRepository;
import com.example.attendance_app.repository.AttendanceIdempotencyKeyRepository;
import com.example.attendance_app.repository.AttendanceRecordRepository;
import com.example.attendance_app.service.support.PageQuerySupport;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_HR = "ROLE_HR";
    private static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;
    private static final int AUDIT_DETAILS_MAX_LENGTH = 2000;
    private static final Set<String> ATTENDANCE_SORT_FIELDS = Set.of(
        "id",
        "arrivalTime",
        "recordedAt",
        "status",
        "checkInSource",
        "distanceMeters"
    );
    private static final Set<String> ATTENDANCE_AUDIT_SORT_FIELDS = Set.of(
        "id",
        "action",
        "actorSubject",
        "actorEmployeeId",
        "createdAt"
    );

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceAuditTrailRepository attendanceAuditTrailRepository;
    private final AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository;
    private final EmployeeService employeeService;
    private final SiteService siteService;
    private final AttendanceRulesProperties attendanceRulesProperties;
    private final Clock clock;

    public AttendanceService(
        AttendanceRecordRepository attendanceRecordRepository,
        AttendanceAuditTrailRepository attendanceAuditTrailRepository,
        AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository,
        EmployeeService employeeService,
        SiteService siteService,
        AttendanceRulesProperties attendanceRulesProperties,
        Clock clock
    ) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceAuditTrailRepository = attendanceAuditTrailRepository;
        this.attendanceIdempotencyKeyRepository = attendanceIdempotencyKeyRepository;
        this.employeeService = employeeService;
        this.siteService = siteService;
        this.attendanceRulesProperties = attendanceRulesProperties;
        this.clock = clock;
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Record a check-in and evaluate geofence status
            ----------------------------------------------------------------
            @parameter: AttendanceCheckInRequest request
            @Returnvalue: AttendanceResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public AttendanceResponse recordCheckIn(AttendanceCheckInRequest request) {
        return recordCheckIn(request, null);
    }

    public AttendanceResponse recordCheckIn(AttendanceCheckInRequest request, String rawIdempotencyKey) {
        enforceSelfCheckInPolicy(request.employeeId());

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

        ZoneId siteZone = resolveSiteZone(site);

        // Fallback defaults when client omits optional check-in metadata.
        Instant arrivalTime = request.arrivalTime() == null ? Instant.now(clock) : request.arrivalTime().toInstant();
        AttendanceSource source = request.checkInSource() == null ? AttendanceSource.MOBILE_APP : request.checkInSource();
        validateArrivalTimeRules(arrivalTime, siteZone);

        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        String requestFingerprint = buildRequestFingerprint(request);
        if (idempotencyKey != null) {
            Optional<AttendanceIdempotencyKey> existingRequest = attendanceIdempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);
            if (existingRequest.isPresent()) {
                return resolveIdempotentReplay(existingRequest.get(), request.employeeId(), requestFingerprint);
            }
        }

        validateDuplicateCheckInRules(employee.getId(), arrivalTime, siteZone, null);

        AttendanceStatus status = AttendanceStatus.UNVERIFIED;
        Double distanceMeters = null;
        if (
            hasLatitude &&
            site.getLatitude() != null &&
            site.getLongitude() != null &&
            site.getGeofenceRadiusMeters() != null
        ) {
            // Compute geofence distance only when all required coordinates are available.
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
        record.setRecordedAt(Instant.now(clock));
        AttendanceRecord savedRecord = attendanceRecordRepository.save(record);

        if (idempotencyKey != null) {
            Optional<AttendanceResponse> replayResponse = registerIdempotencyOrReplay(
                idempotencyKey,
                requestFingerprint,
                employee,
                savedRecord
            );
            if (replayResponse.isPresent()) {
                return replayResponse.get();
            }
        }

        AuditActor actor = resolveAuditActor(getCurrentAuthentication());
        saveAuditEntry(
            savedRecord,
            AttendanceAuditAction.CREATED,
            null,
            "source=" + savedRecord.getCheckInSource() + ";status=" + savedRecord.getStatus(),
            actor
        );

        return mapToResponse(savedRecord);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Retrieve one attendance record by id
            ----------------------------------------------------------------
            @parameter: Long id
            @Returnvalue: AttendanceResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceById(Long id) {
        AttendanceRecord record = attendanceRecordRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id));
        return mapToResponse(record);
    }

    public AttendanceResponse correctAttendance(Long id, AttendanceCorrectionRequest request) {
        enforceAttendanceManagementPolicy();

        AttendanceRecord record = attendanceRecordRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id));

        if (record.getStatus() == AttendanceStatus.CANCELLED) {
            throw new ConflictException("Cancelled attendance records cannot be corrected");
        }
        if (request.status() == AttendanceStatus.CANCELLED) {
            throw new BadRequestException("Use cancellation endpoint to cancel an attendance record");
        }

        ZoneId siteZone = resolveSiteZone(record.getSite());

        Instant previousArrivalTime = record.getArrivalTime();
        AttendanceSource previousSource = record.getCheckInSource();
        AttendanceStatus previousStatus = record.getStatus();
        String previousNotes = record.getNotes();

        Instant correctedArrivalTime = request.arrivalTime() == null ? record.getArrivalTime() : request.arrivalTime().toInstant();
        AttendanceSource correctedSource = request.checkInSource() == null ? record.getCheckInSource() : request.checkInSource();
        AttendanceStatus correctedStatus = request.status() == null ? record.getStatus() : request.status();
        String correctedNotes = request.notes() == null ? record.getNotes() : trimToNull(request.notes());

        boolean changed = !previousArrivalTime.equals(correctedArrivalTime)
            || previousSource != correctedSource
            || previousStatus != correctedStatus
            || !java.util.Objects.equals(previousNotes, correctedNotes);

        if (!changed) {
            throw new BadRequestException("No correction detected. At least one field must change");
        }

        validateArrivalTimeRules(correctedArrivalTime, siteZone);
        validateDuplicateCheckInRules(record.getEmployee().getId(), correctedArrivalTime, siteZone, record.getId());

        record.setArrivalTime(correctedArrivalTime);
        record.setCheckInSource(correctedSource);
        record.setStatus(correctedStatus);
        record.setNotes(correctedNotes);

        AttendanceRecord savedRecord = attendanceRecordRepository.save(record);

        String reason = normalizeRequiredReason(request.reason());
        String details = "arrivalTimeUtc:" + previousArrivalTime + "->" + correctedArrivalTime
            + ";source:" + previousSource + "->" + correctedSource
            + ";status:" + previousStatus + "->" + correctedStatus
            + ";notesChanged:" + !java.util.Objects.equals(previousNotes, correctedNotes);

        saveAuditEntry(
            savedRecord,
            AttendanceAuditAction.CORRECTED,
            reason,
            details,
            resolveAuditActor(getCurrentAuthentication())
        );

        return mapToResponse(savedRecord);
    }

    public AttendanceResponse cancelAttendance(Long id, AttendanceCancellationRequest request) {
        enforceAttendanceManagementPolicy();

        AttendanceRecord record = attendanceRecordRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id));

        if (record.getStatus() == AttendanceStatus.CANCELLED) {
            throw new ConflictException("Attendance record is already cancelled");
        }

        AttendanceStatus previousStatus = record.getStatus();
        record.setStatus(AttendanceStatus.CANCELLED);
        AttendanceRecord savedRecord = attendanceRecordRepository.save(record);

        String reason = normalizeRequiredReason(request.reason());
        String details = "status:" + previousStatus + "->" + AttendanceStatus.CANCELLED;

        saveAuditEntry(
            savedRecord,
            AttendanceAuditAction.CANCELLED,
            reason,
            details,
            resolveAuditActor(getCurrentAuthentication())
        );

        return mapToResponse(savedRecord);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AttendanceAuditResponse> getAttendanceAuditTrail(
        Long attendanceId,
        int page,
        int size,
        String sortBy,
        String sortDir
    ) {
        enforceAttendanceManagementPolicy();

        if (!attendanceRecordRepository.existsById(attendanceId)) {
            throw new ResourceNotFoundException("Attendance record not found: " + attendanceId);
        }

        Pageable pageable = PageQuerySupport.buildPageable(
            page,
            size,
            sortBy,
            sortDir,
            ATTENDANCE_AUDIT_SORT_FIELDS,
            "createdAt",
            Sort.Direction.DESC
        );

        Page<AttendanceAuditResponse> auditPage = attendanceAuditTrailRepository
            .findByAttendanceRecordId(attendanceId, pageable)
            .map(this::mapToAuditResponse);

        return PagedResponse.from(auditPage);
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Search attendance with optional filters
            ----------------------------------------------------------------
            @parameter: employeeId, siteId, from, to, status, source, query, page, size, sortBy, sortDir
            @Returnvalue: PagedResponse<AttendanceResponse>
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    @Transactional(readOnly = true)
    public PagedResponse<AttendanceResponse> searchAttendance(
        Long employeeId,
        Long siteId,
        OffsetDateTime from,
        OffsetDateTime to,
        AttendanceStatus status,
        AttendanceSource source,
        String query,
        int page,
        int size,
        String sortBy,
        String sortDir
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be earlier than or equal to to");
        }
        Instant fromInstant = from == null ? null : from.toInstant();
        Instant toInstant = to == null ? null : to.toInstant();

        Pageable pageable = PageQuerySupport.buildPageable(
            page,
            size,
            sortBy,
            sortDir,
            ATTENDANCE_SORT_FIELDS,
            "arrivalTime",
            Sort.Direction.DESC
        );

        // Start from a neutral predicate, then append optional constraints.
        Specification<AttendanceRecord> specification = (root, criteriaQuery, cb) -> cb.conjunction();

        if (employeeId != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }
        if (siteId != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.equal(root.get("site").get("id"), siteId));
        }
        if (fromInstant != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.greaterThanOrEqualTo(root.get("arrivalTime"), fromInstant));
        }
        if (toInstant != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.lessThanOrEqualTo(root.get("arrivalTime"), toInstant));
        }
        if (status != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.equal(root.get("status"), status));
        }
        if (source != null) {
            specification = specification.and((root, criteriaQuery, cb) -> cb.equal(root.get("checkInSource"), source));
        }
        if (query != null && !query.isBlank()) {
            String keyword = "%" + query.trim().toLowerCase() + "%";
            specification = specification.and((root, criteriaQuery, cb) -> cb.or(
                cb.like(cb.lower(root.get("employee").get("employeeCode")), keyword),
                cb.like(cb.lower(root.get("site").get("code")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), keyword)
            ));
        }

        Page<AttendanceResponse> result = attendanceRecordRepository.findAll(specification, pageable)
            .map(this::mapToResponse);
        return PagedResponse.from(result);
    }

    private String normalizeIdempotencyKey(String rawIdempotencyKey) {
        if (rawIdempotencyKey == null) {
            return null;
        }
        String normalizedKey = rawIdempotencyKey.trim();
        if (normalizedKey.isEmpty()) {
            throw new BadRequestException("Idempotency-Key cannot be blank");
        }
        if (normalizedKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new BadRequestException("Idempotency-Key must not exceed " + IDEMPOTENCY_KEY_MAX_LENGTH + " characters");
        }
        return normalizedKey;
    }

    private String buildRequestFingerprint(AttendanceCheckInRequest request) {
        String canonicalPayload = String.join(
            "|",
            String.valueOf(request.employeeId()),
            String.valueOf(request.siteId()),
            request.arrivalTime() == null ? "AUTO" : request.arrivalTime().toInstant().toString(),
            request.checkInSource() == null ? "AUTO" : request.checkInSource().name(),
            request.latitude() == null ? "NULL" : request.latitude().toString(),
            request.longitude() == null ? "NULL" : request.longitude().toString(),
            normalizeNotes(request.notes())
        );
        return sha256Hex(canonicalPayload);
    }

    private AttendanceResponse resolveIdempotentReplay(
        AttendanceIdempotencyKey existingRequest,
        Long targetEmployeeId,
        String expectedFingerprint
    ) {
        if (!existingRequest.getEmployee().getId().equals(targetEmployeeId)) {
            throw new ConflictException("Idempotency-Key is already associated with a different employee");
        }
        if (!existingRequest.getRequestFingerprint().equals(expectedFingerprint)) {
            throw new ConflictException("Idempotency-Key was already used with different request data");
        }
        return mapToResponse(existingRequest.getAttendanceRecord());
    }

    private Optional<AttendanceResponse> registerIdempotencyOrReplay(
        String idempotencyKey,
        String requestFingerprint,
        Employee employee,
        AttendanceRecord attendanceRecord
    ) {
        AttendanceIdempotencyKey idempotency = new AttendanceIdempotencyKey();
        idempotency.setIdempotencyKey(idempotencyKey);
        idempotency.setEmployee(employee);
        idempotency.setAttendanceRecord(attendanceRecord);
        idempotency.setRequestFingerprint(requestFingerprint);

        try {
            attendanceIdempotencyKeyRepository.saveAndFlush(idempotency);
            return Optional.empty();
        } catch (DataIntegrityViolationException exception) {
            AttendanceIdempotencyKey existingRequest = attendanceIdempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ConflictException("Idempotency-Key already used"));
            return Optional.of(resolveIdempotentReplay(existingRequest, employee.getId(), requestFingerprint));
        }
    }

    private void validateDuplicateCheckInRules(Long employeeId, Instant arrivalTime, ZoneId siteZone, Long excludedRecordId) {
        if (!attendanceRulesProperties.isDuplicateGuardEnabled()) {
            return;
        }

        if (attendanceRulesProperties.isSingleCheckInPerDay()) {
            Instant dayStart = arrivalTime
                .atZone(siteZone)
                .toLocalDate()
                .atStartOfDay(siteZone)
                .toInstant();
            Instant dayEnd = arrivalTime
                .atZone(siteZone)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(siteZone)
                .minusNanos(1)
                .toInstant();
            Optional<AttendanceRecord> sameDayRecord = attendanceRecordRepository
                .findTopByEmployeeIdAndIdNotAndArrivalTimeBetweenOrderByArrivalTimeDesc(
                    employeeId,
                    excludedRecordId == null ? -1L : excludedRecordId,
                    dayStart,
                    dayEnd
                );
            if (sameDayRecord.isPresent()) {
                AttendanceRecord duplicate = sameDayRecord.get();
                throw new ConflictException(
                    "Duplicate check-in detected for this day. existingRecordId="
                        + duplicate.getId()
                        + ", existingArrivalTime="
                        + duplicate.getArrivalTime().atZone(siteZone).toOffsetDateTime().format(DATE_TIME_FORMATTER)
                );
            }
        }

        long minMinutesBetweenCheckIns = attendanceRulesProperties.getMinMinutesBetweenCheckIns();
        if (minMinutesBetweenCheckIns > 0) {
            Instant from = arrivalTime.minusSeconds(minMinutesBetweenCheckIns * 60);
            Instant to = arrivalTime.plusSeconds(minMinutesBetweenCheckIns * 60);
            Optional<AttendanceRecord> nearbyRecord = attendanceRecordRepository
                .findTopByEmployeeIdAndIdNotAndArrivalTimeBetweenOrderByArrivalTimeDesc(
                    employeeId,
                    excludedRecordId == null ? -1L : excludedRecordId,
                    from,
                    to
                );
            if (nearbyRecord.isPresent()) {
                AttendanceRecord duplicate = nearbyRecord.get();
                throw new ConflictException(
                    "Duplicate check-in detected within interval of "
                        + minMinutesBetweenCheckIns
                        + " minutes. existingRecordId="
                        + duplicate.getId()
                );
            }
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null) {
            return "";
        }
        return notes.trim();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }

    private void saveAuditEntry(
        AttendanceRecord attendanceRecord,
        AttendanceAuditAction action,
        String reason,
        String details,
        AuditActor actor
    ) {
        AttendanceAuditTrail auditEntry = new AttendanceAuditTrail();
        auditEntry.setAttendanceRecord(attendanceRecord);
        auditEntry.setAction(action);
        auditEntry.setActorSubject(actor.subject());
        auditEntry.setActorEmployeeId(actor.employeeId());
        auditEntry.setActorRoles(actor.roles());
        auditEntry.setReason(trimToNull(reason));
        auditEntry.setDetails(truncateDetails(details));
        auditEntry.setCreatedAt(Instant.now(clock));
        attendanceAuditTrailRepository.save(auditEntry);
    }

    private String truncateDetails(String details) {
        String normalizedDetails = trimToNull(details);
        if (normalizedDetails == null) {
            return null;
        }
        if (normalizedDetails.length() <= AUDIT_DETAILS_MAX_LENGTH) {
            return normalizedDetails;
        }
        return normalizedDetails.substring(0, AUDIT_DETAILS_MAX_LENGTH);
    }

    private String normalizeRequiredReason(String reason) {
        String normalizedReason = trimToNull(reason);
        if (normalizedReason == null) {
            throw new BadRequestException("reason is required");
        }
        return normalizedReason;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Authentication getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authenticated context is required");
        }
        return authentication;
    }

    private void enforceAttendanceManagementPolicy() {
        Authentication authentication = getCurrentAuthentication();
        boolean allowed = authentication.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .anyMatch(role -> ROLE_ADMIN.equals(role) || ROLE_HR.equals(role));
        if (!allowed) {
            throw new AccessDeniedException("Only ADMIN or HR can correct/cancel attendance records");
        }
    }

    private AuditActor resolveAuditActor(Authentication authentication) {
        String subject = resolveActorSubject(authentication);
        Long employeeId = resolveAuthenticatedEmployeeId(authentication);
        String roles = authentication.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .sorted()
            .collect(Collectors.joining(","));

        if (roles.isBlank()) {
            roles = "UNKNOWN";
        }
        return new AuditActor(subject, employeeId, roles);
    }

    private String resolveActorSubject(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String jwtSubject = trimToNull(jwt.getSubject());
            if (jwtSubject != null) {
                return jwtSubject;
            }
        }

        String username = trimToNull(authentication.getName());
        if (username != null) {
            return username;
        }
        return "unknown";
    }

    private AttendanceAuditResponse mapToAuditResponse(AttendanceAuditTrail auditTrail) {
        return new AttendanceAuditResponse(
            auditTrail.getId(),
            auditTrail.getAttendanceRecord().getId(),
            auditTrail.getAction(),
            auditTrail.getActorSubject(),
            auditTrail.getActorEmployeeId(),
            auditTrail.getActorRoles(),
            auditTrail.getReason(),
            auditTrail.getDetails(),
            auditTrail.getCreatedAt()
        );
    }

    private void enforceSelfCheckInPolicy(Long targetEmployeeId) {
        Authentication authentication = getCurrentAuthentication();

        Set<String> authorities = authentication.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .collect(Collectors.toSet());

        if (authorities.contains(ROLE_ADMIN) || authorities.contains(ROLE_HR)) {
            return;
        }

        if (!authorities.contains(ROLE_EMPLOYEE)) {
            throw new AccessDeniedException("User role is not allowed to perform attendance check-in");
        }

        Long authenticatedEmployeeId = resolveAuthenticatedEmployeeId(authentication);
        if (authenticatedEmployeeId == null) {
            throw new AccessDeniedException("Employee identity claim is required for self check-in");
        }
        if (!authenticatedEmployeeId.equals(targetEmployeeId)) {
            throw new AccessDeniedException("Employees can only record attendance for themselves");
        }
    }

    private record AuditActor(String subject, Long employeeId, String roles) {
    }

    private Long resolveAuthenticatedEmployeeId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return null;
        }

        Long fromIdClaim = extractLongClaim(jwt, "employeeId")
            .or(() -> extractLongClaim(jwt, "employee_id"))
            .orElse(null);
        if (fromIdClaim != null) {
            return fromIdClaim;
        }

        String identifier = extractStringClaim(jwt, "employeeCode")
            .or(() -> extractStringClaim(jwt, "employee_code"))
            .or(() -> extractStringClaim(jwt, "email"))
            .or(() -> extractStringClaim(jwt, "preferred_username"))
            .orElseGet(jwt::getSubject);

        return employeeService.resolveEmployeeIdByIdentifier(identifier).orElse(null);
    }

    private Optional<Long> extractLongClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaims().get(claimName);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        if (value instanceof String textValue && !textValue.isBlank()) {
            try {
                return Optional.of(Long.parseLong(textValue.trim()));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractStringClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaims().get(claimName);
        if (value instanceof String textValue && !textValue.isBlank()) {
            return Optional.of(textValue.trim());
        }
        return Optional.empty();
    }

    private void validateArrivalTimeRules(Instant arrivalTime, ZoneId siteZone) {
        Instant now = Instant.now(clock);

        Instant latestAllowed = now.plusSeconds(attendanceRulesProperties.getMaxFutureToleranceSeconds());
        if (arrivalTime.isAfter(latestAllowed)) {
            throw new BadRequestException(
                "arrivalTime cannot be in the future. max allowed="
                    + latestAllowed.toString()
            );
        }

        Instant oldestAllowed = now.minusSeconds(attendanceRulesProperties.getMaxRetroactiveMinutes() * 60);
        if (arrivalTime.isBefore(oldestAllowed)) {
            throw new BadRequestException(
                "arrivalTime is too old. oldest allowed="
                    + oldestAllowed.toString()
            );
        }

        if (attendanceRulesProperties.isEnforceDailyWindow()) {
            LocalTime value = arrivalTime.atZone(siteZone).toLocalTime();
            LocalTime windowStart = attendanceRulesProperties.getDailyWindowStart();
            LocalTime windowEnd = attendanceRulesProperties.getDailyWindowEnd();
            if (!isWithinDailyWindow(value, windowStart, windowEnd)) {
                throw new BadRequestException(
                    "arrivalTime is outside allowed daily window ("
                        + windowStart
                        + " - "
                        + windowEnd
                        + ")"
                );
            }
        }
    }

    private boolean isWithinDailyWindow(LocalTime value, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !value.isBefore(start) && !value.isAfter(end);
        }
        return !value.isBefore(start) || !value.isAfter(end);
    }

    private ZoneId resolveSiteZone(CompanySite site) {
        String timezone = site.getTimezone();
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException exception) {
            return ZoneOffset.UTC;
        }
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Map entity model to API response DTO
            ----------------------------------------------------------------
            @parameter: AttendanceRecord record
            @Returnvalue: AttendanceResponse
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private AttendanceResponse mapToResponse(AttendanceRecord record) {
        Boolean withinSiteRange = null;
        if (record.getStatus() == AttendanceStatus.ON_SITE) {
            withinSiteRange = true;
        } else if (record.getStatus() == AttendanceStatus.OFF_SITE) {
            withinSiteRange = false;
        }
        ZoneId siteZone = resolveSiteZone(record.getSite());
        Instant recordedAtUtc = record.getRecordedAt() == null ? record.getArrivalTime() : record.getRecordedAt();
        OffsetDateTime arrivalTimeLocal = record.getArrivalTime().atZone(siteZone).toOffsetDateTime();
        OffsetDateTime recordedAtLocal = recordedAtUtc.atZone(siteZone).toOffsetDateTime();

        return new AttendanceResponse(
            record.getId(),
            record.getEmployee().getId(),
            record.getEmployee().getEmployeeCode(),
            record.getSite().getId(),
            record.getSite().getCode(),
            record.getArrivalTime(),
            arrivalTimeLocal,
            record.getCheckInSource(),
            record.getStatus(),
            record.getLatitude(),
            record.getLongitude(),
            record.getDistanceMeters(),
            withinSiteRange,
            record.getNotes(),
            recordedAtUtc,
            recordedAtLocal,
            siteZone.getId()
        );
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            @Function Description: Calculate distance between two points in meters
            ----------------------------------------------------------------
            @parameter: lat1, lon1, lat2, lon2
            @Returnvalue: double
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
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
