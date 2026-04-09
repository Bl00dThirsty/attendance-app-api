package com.example.attendance_app.integration;

import com.example.attendance_app.entity.AttendanceAuditAction;
import com.example.attendance_app.entity.AttendanceAuditTrail;
import com.example.attendance_app.entity.AttendanceIdempotencyKey;
import com.example.attendance_app.entity.AttendanceRecord;
import com.example.attendance_app.entity.AttendanceSource;
import com.example.attendance_app.entity.AttendanceStatus;
import com.example.attendance_app.entity.CompanySite;
import com.example.attendance_app.entity.Department;
import com.example.attendance_app.entity.Employee;
import com.example.attendance_app.entity.EmployeeRole;
import com.example.attendance_app.entity.JobPosition;
import com.example.attendance_app.repository.AttendanceAuditTrailRepository;
import com.example.attendance_app.repository.AttendanceIdempotencyKeyRepository;
import com.example.attendance_app.repository.AttendanceRecordRepository;
import com.example.attendance_app.repository.CompanySiteRepository;
import com.example.attendance_app.repository.DepartmentRepository;
import com.example.attendance_app.repository.EmployeeRepository;
import com.example.attendance_app.repository.JobPositionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class AttendancePostgresContainerIntegrationTests {

    private static final String TEST_JWT_SECRET = "YXR0ZW5kYW5jZS10ZXN0LXNlY3JldC1rZXktMzItYnl0ZXM=";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("attendance_test")
        .withUsername("attendance")
        .withPassword("attendance");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> "false");

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/postgresql");

        registry.add("springdoc.swagger-ui.enabled", () -> "false");
        registry.add("springdoc.api-docs.enabled", () -> "false");
        registry.add("spring.h2.console.enabled", () -> "false");

        registry.add("app.security.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.security.jwt.issuer", () -> "cale-auth-service");
        registry.add("app.security.jwt.audience", () -> "attendance-app-api");
        registry.add("app.security.cors.allowed-origins", () -> "http://localhost:4200");
    }

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private AttendanceAuditTrailRepository attendanceAuditTrailRepository;

    @Autowired
    private AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JobPositionRepository jobPositionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CompanySiteRepository companySiteRepository;

    private Employee employee;
    private CompanySite site;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        Department department = new Department();
        department.setCode("DPT-OPS");
        department.setName("Operations");
        department.setActive(true);
        department = departmentRepository.save(department);

        JobPosition position = new JobPosition();
        position.setCode("POS-SUP");
        position.setName("Supervisor");
        position.setActive(true);
        position = jobPositionRepository.save(position);

        Employee savedEmployee = new Employee();
        savedEmployee.setEmployeeCode("EMP-PG-01");
        savedEmployee.setFirstName("Docker");
        savedEmployee.setLastName("Tester");
        savedEmployee.setEmail("docker.tester@cale.local");
        savedEmployee.setDepartment(department);
        savedEmployee.setPosition(position);
        savedEmployee.setRole(EmployeeRole.EMPLOYEE);
        savedEmployee.setActive(true);
        employee = employeeRepository.save(savedEmployee);

        CompanySite savedSite = new CompanySite();
        savedSite.setCode("SITE-PG");
        savedSite.setName("Postgres Site");
        savedSite.setAddress("Akwa");
        savedSite.setTimezone("Africa/Douala");
        savedSite.setGeofenceRadiusMeters(200);
        savedSite.setActive(true);
        site = companySiteRepository.save(savedSite);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldEnforceUniqueIdempotencyKeyConstraint() {
        AttendanceRecord firstRecord = attendanceRecordRepository.save(buildRecord(Instant.parse("2026-04-09T08:00:00Z")));
        AttendanceRecord secondRecord = attendanceRecordRepository.save(buildRecord(Instant.parse("2026-04-09T10:00:00Z")));

        AttendanceIdempotencyKey firstKey = new AttendanceIdempotencyKey();
        firstKey.setIdempotencyKey("idem-postgres-001");
        firstKey.setEmployee(employee);
        firstKey.setAttendanceRecord(firstRecord);
        firstKey.setRequestFingerprint("f".repeat(64));
        attendanceIdempotencyKeyRepository.saveAndFlush(firstKey);

        AttendanceIdempotencyKey duplicatedKey = new AttendanceIdempotencyKey();
        duplicatedKey.setIdempotencyKey("idem-postgres-001");
        duplicatedKey.setEmployee(employee);
        duplicatedKey.setAttendanceRecord(secondRecord);
        duplicatedKey.setRequestFingerprint("a".repeat(64));

        assertThrows(
            DataIntegrityViolationException.class,
            () -> attendanceIdempotencyKeyRepository.saveAndFlush(duplicatedKey)
        );
    }

    @Test
    void shouldPersistAndQueryAuditTrailByAttendanceRecord() {
        AttendanceRecord record = attendanceRecordRepository.save(buildRecord(Instant.parse("2026-04-09T09:00:00Z")));

        AttendanceAuditTrail createdAudit = new AttendanceAuditTrail();
        createdAudit.setAttendanceRecord(record);
        createdAudit.setAction(AttendanceAuditAction.CREATED);
        createdAudit.setActorSubject("system");
        createdAudit.setActorRoles("ROLE_ADMIN");
        createdAudit.setReason("initial load");
        createdAudit.setDetails("source=WEB_TERMINAL");
        createdAudit.setCreatedAt(Instant.parse("2026-04-09T09:00:30Z"));
        attendanceAuditTrailRepository.save(createdAudit);

        AttendanceAuditTrail correctedAudit = new AttendanceAuditTrail();
        correctedAudit.setAttendanceRecord(record);
        correctedAudit.setAction(AttendanceAuditAction.CORRECTED);
        correctedAudit.setActorSubject("admin.user");
        correctedAudit.setActorRoles("ROLE_ADMIN");
        correctedAudit.setReason("clock drift");
        correctedAudit.setDetails("arrivalTime corrected");
        correctedAudit.setCreatedAt(Instant.parse("2026-04-09T09:10:30Z"));
        attendanceAuditTrailRepository.save(correctedAudit);

        var result = attendanceAuditTrailRepository.findByAttendanceRecordId(
            record.getId(),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertEquals(2, result.getTotalElements());
        assertEquals(AttendanceAuditAction.CORRECTED, result.getContent().getFirst().getAction());
        assertEquals(AttendanceAuditAction.CREATED, result.getContent().get(1).getAction());
    }

    private AttendanceRecord buildRecord(Instant arrivalTime) {
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployee(employee);
        record.setSite(site);
        record.setArrivalTime(arrivalTime);
        record.setCheckInSource(AttendanceSource.WEB_TERMINAL);
        record.setStatus(AttendanceStatus.UNVERIFIED);
        record.setNotes("postgres-integration-test");
        record.setRecordedAt(arrivalTime.plusSeconds(10));
        return record;
    }

    private void cleanDatabase() {
        attendanceAuditTrailRepository.deleteAllInBatch();
        attendanceIdempotencyKeyRepository.deleteAllInBatch();
        attendanceRecordRepository.deleteAllInBatch();
        employeeRepository.deleteAllInBatch();
        companySiteRepository.deleteAllInBatch();
        jobPositionRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
    }
}
