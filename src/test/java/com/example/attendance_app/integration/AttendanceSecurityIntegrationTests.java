package com.example.attendance_app.integration;

import com.example.attendance_app.entity.AttendanceSource;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AttendanceSecurityIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AttendanceAuditTrailRepository attendanceAuditTrailRepository;

    @Autowired
    private AttendanceIdempotencyKeyRepository attendanceIdempotencyKeyRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private CompanySiteRepository companySiteRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JobPositionRepository jobPositionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private MockMvc mockMvc;
    private Long employeeOneId;
    private Long employeeTwoId;
    private Long siteId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        cleanDatabase();

        Department department = new Department();
        department.setCode("DPT-IT");
        department.setName("IT");
        department.setActive(true);
        department = departmentRepository.save(department);

        JobPosition position = new JobPosition();
        position.setCode("POS-DEV");
        position.setName("Developer");
        position.setDescription("Software Engineer");
        position.setActive(true);
        position = jobPositionRepository.save(position);

        Employee employeeOne = new Employee();
        employeeOne.setEmployeeCode("EMP-0001");
        employeeOne.setFirstName("Alice");
        employeeOne.setLastName("One");
        employeeOne.setEmail("alice.one@cale.local");
        employeeOne.setDepartment(department);
        employeeOne.setPosition(position);
        employeeOne.setRole(EmployeeRole.EMPLOYEE);
        employeeOne.setActive(true);
        employeeOne = employeeRepository.save(employeeOne);
        employeeOneId = employeeOne.getId();

        Employee employeeTwo = new Employee();
        employeeTwo.setEmployeeCode("EMP-0002");
        employeeTwo.setFirstName("Bob");
        employeeTwo.setLastName("Two");
        employeeTwo.setEmail("bob.two@cale.local");
        employeeTwo.setDepartment(department);
        employeeTwo.setPosition(position);
        employeeTwo.setRole(EmployeeRole.EMPLOYEE);
        employeeTwo.setActive(true);
        employeeTwo = employeeRepository.save(employeeTwo);
        employeeTwoId = employeeTwo.getId();

        CompanySite site = new CompanySite();
        site.setCode("SITE-HQ");
        site.setName("Headquarters");
        site.setAddress("Douala");
        site.setTimezone("Africa/Douala");
        site.setGeofenceRadiusMeters(300);
        site.setLatitude(4.0511);
        site.setLongitude(9.7679);
        site.setActive(true);
        site = companySiteRepository.save(site);
        siteId = site.getId();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void checkInShouldReturnUnauthorizedWhenJwtIsMissing() throws Exception {
        String payload = checkInPayload(employeeOneId, siteId);

        mockMvc.perform(
                post("/api/attendance/check-in")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void departmentsEndpointShouldRejectEmployeeRole() throws Exception {
        mockMvc.perform(
                get("/api/departments")
                    .with(employeeJwt(employeeOneId))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void employeeShouldOnlyCheckInForSelf() throws Exception {
        mockMvc.perform(
                post("/api/attendance/check-in")
                    .with(employeeJwt(employeeOneId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInPayload(employeeTwoId, siteId))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void employeeShouldCheckInSuccessfullyForOwnIdentity() throws Exception {
        mockMvc.perform(
                post("/api/attendance/check-in")
                    .with(employeeJwt(employeeOneId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInPayload(employeeOneId, siteId))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.employeeId").value(employeeOneId))
            .andExpect(jsonPath("$.siteId").value(siteId))
            .andExpect(jsonPath("$.checkInSource").value(AttendanceSource.MOBILE_APP.name()));
    }

    private RequestPostProcessor employeeJwt(Long employeeId) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
            .jwt(jwt -> jwt
                .subject("employee-" + employeeId)
                .claim("roles", List.of("EMPLOYEE"))
                .claim("employeeId", employeeId)
            );
    }

    private String checkInPayload(Long employeeIdValue, Long siteIdValue) {
        String arrivalTime = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2).toString();
        return """
            {
              "employeeId": %d,
              "siteId": %d,
              "arrivalTime": "%s",
              "checkInSource": "MOBILE_APP",
              "notes": "integration-security-test"
            }
            """.formatted(employeeIdValue, siteIdValue, arrivalTime);
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
