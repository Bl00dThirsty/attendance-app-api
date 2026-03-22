# Backend README - Attendance App

## Overview

The backend is a Spring Boot REST API that manages:

- Employees
- Company sites
- Attendance check-ins
- Role-based authorization

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Security (HTTP Basic)
- Spring Data JPA
- H2 in-memory database
- Springdoc OpenAPI (Swagger)

## Architecture Principles

The backend follows a layered architecture:

- `controller`: REST endpoints and request mapping
- `service`: business rules and orchestration
- `repository`: data access interfaces
- `entity`: JPA domain model
- `dto`: API request/response contracts
- `config`: infrastructure (security, docs)

This separation keeps business logic testable and avoids controller bloat.

## Domain Model

Main entities:

- `Employee`
  - `employeeCode`, `firstName`, `lastName`, `email`
  - optional relations to `JobPosition` and `Department`
  - HR/personnel fields: `hireDate`, `birthDate`, `birthPlace`, `contractType`, `employeeType`, `maritalStatus`, `gender`
  - residence and contact fields: `cityOfResidence`, `district`, `address`, `phoneNumber`, emergency contact
  - identity fields: `nationality`, `nationalIdNumber`
  - contract lifecycle: `contractStartDate`, `contractEndDate`
  - `role`: `EMPLOYEE`, `HR`, `ADMIN`
  - `active`
- `JobPosition`
  - `code`, `name`, optional `description`
  - `active`
- `Department`
  - `code`, `name`
  - `active`
- `CompanySite`
  - `code`, `name`, `address`
  - optional GPS + geofence radius
- `AttendanceRecord`
  - relation to employee + site
  - `arrivalTime`, `checkInSource`, status and distance data

## Security and Roles

Authentication is HTTP Basic with in-memory demo users:

- `admin / admin123` -> role `ADMIN`
- `rh / rh123` -> role `HR`
- `employee / employee123` -> role `EMPLOYEE`

Authorization matrix:

- `POST /api/employees` -> `ADMIN`
- `PUT /api/employees/{id}` -> `ADMIN`
- `DELETE /api/employees/{id}` -> `ADMIN`
- `GET /api/employees/**` -> `ADMIN`, `HR`
- `POST /api/departments` -> `ADMIN`
- `GET /api/departments/**` -> `ADMIN`
- `POST /api/positions` -> `ADMIN`
- `PUT /api/positions/{id}` -> `ADMIN`
- `DELETE /api/positions/{id}` -> `ADMIN`
- `GET /api/positions/**` -> `ADMIN`, `HR`
- `POST /api/sites` -> `ADMIN`
- `PUT /api/sites/{id}` -> `ADMIN`
- `DELETE /api/sites/{id}` -> `ADMIN`
- `GET /api/sites/**` -> `ADMIN`, `HR`, `EMPLOYEE`
- `POST /api/attendance/check-in` -> `ADMIN`, `HR`, `EMPLOYEE`
- `GET /api/attendance/**` -> `ADMIN`, `HR`

CORS is enabled for Angular dev origins:

- `http://localhost:4200`
- `http://127.0.0.1:4200`

## Geofencing Principle

When check-in payload contains latitude + longitude and the target site has geofence settings:

- API computes distance between employee point and site point.
- Status is:
  - `ON_SITE` if inside radius
  - `OFF_SITE` if outside radius
- If coordinates are missing, status stays `UNVERIFIED`.

## Run Backend

From repository root:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

API base URL:

- `http://localhost:8080`

## Run With Docker

Build the image:

```bash
docker build -t attendance-app-api .
```

Run the container:

```bash
docker run --rm -p 8080:8080 --name attendance-app-api attendance-app-api
```

Run with Docker Compose (recommended):

```bash
docker compose up --build -d
```

Stop Docker Compose:

```bash
docker compose down
```

Notes:

- Compose stores H2 data in a Docker volume (`attendance-h2-data`) using file mode.
- API stays available at `http://localhost:8080`.
- Swagger: `http://localhost:8080/swagger-ui.html`
- H2 console: `http://localhost:8080/h2-console`

## API and Tools

- Swagger: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- H2 console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:attendance_db`
  - User: `sa`
  - Password: empty

## Build and Test

Build:

```bash
./mvnw clean package
```

Run tests:

```bash
./mvnw test
```

Important: Java 21 is mandatory for compilation.

## Example Requests

Create department as admin:

```bash
curl -u admin:admin123 -X POST http://localhost:8080/api/departments \
  -H "Content-Type: application/json" \
  -d '{
    "code": "HR",
    "name": "Human Resources",
    "active": true
  }'
```

Create position as admin:

```bash
curl -u admin:admin123 -X POST http://localhost:8080/api/positions \
  -H "Content-Type: application/json" \
  -d '{
    "code": "HR_MANAGER",
    "name": "HR Manager",
    "description": "Leads HR operations",
    "active": true
  }'
```

Create employee as admin:

```bash
curl -u admin:admin123 -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "employeeCode": "EMP-100",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@company.com",
    "positionId": 1,
    "departmentId": 1,
    "hireDate": "2025-01-02",
    "birthDate": "1996-03-14",
    "birthPlace": "Douala",
    "contractType": "CDI",
    "employeeType": "FULL_TIME",
    "maritalStatus": "MARRIED",
    "gender": "MALE",
    "cityOfResidence": "Douala",
    "district": "Bonapriso",
    "nationality": "Cameroonian",
    "phoneNumber": "+237690000000",
    "address": "Bonapriso, Douala",
    "contractStartDate": "2025-01-02",
    "emergencyContactName": "Jane Doe",
    "emergencyContactPhone": "+237691111111",
    "role": "HR",
    "active": true
  }'
```

Record check-in as employee:

```bash
curl -u employee:employee123 -X POST http://localhost:8080/api/attendance/check-in \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 1,
    "siteId": 1,
    "checkInSource": "MOBILE_APP"
  }'
```

## Troubleshooting

- `release version 21 not supported`
  - Install Java 21 and ensure `java -version` reports 21.
- `401 Unauthorized`
  - Verify HTTP Basic credentials.
- `403 Forbidden`
  - Your role is not allowed for this endpoint.
- Frontend cannot call API
  - Ensure backend is running on port 8080 and frontend on 4200.
