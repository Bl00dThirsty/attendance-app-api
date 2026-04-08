# Attendance API (ERP Domain Service)

This repository contains only the attendance domain API.
Authentication is now fully externalized in a dedicated repository:

- `C:\Users\User\Documents\DEV\DEV-FREE\cale-auth-service`

## Scope of this service

- Employees management
- Departments management
- Job positions management
- Company sites and geofencing
- Attendance check-in and search

## List endpoints (pagination / sorting / filtering)

All list endpoints support pagination and sorting query parameters:

- `page` (default: `0`)
- `size` (default: `20`, max: `200`)
- `sortBy` (endpoint-specific whitelist)
- `sortDir` (`ASC` or `DESC`, default: `DESC`)

Filtering/search:

- `/api/employees`: `q`, `departmentId`, `positionId`, `active`
- `/api/departments`: `q`, `active`
- `/api/positions`: `q`, `active`
- `/api/sites`: `q`, `active`
- `/api/attendance`: `employeeId`, `siteId`, `from`, `to`, `status`, `source`, `q`
  - `from` and `to` use ISO-8601 with offset (example: `2026-04-08T08:00:00+01:00`)

## Attendance timezone normalization

- Attendance timestamps are persisted in UTC (`Instant`) at database level.
- Check-in input timestamp is `OffsetDateTime` (timezone-aware).
- API response exposes both UTC and localized values:
  - `arrivalTimeUtc`, `recordedAtUtc`
  - `arrivalTimeLocal`, `recordedAtLocal`
  - `siteTimezone`
- Each site has a dedicated IANA timezone (`company_sites.timezone`, example: `Africa/Douala`).
- If no timezone is provided when creating a site, the service defaults to `UTC`.

## Attendance business rules

Check-in validation is enforced before persistence:

- future check-ins are rejected (`maxFutureToleranceSeconds`)
- excessive backdating is rejected (`maxRetroactiveMinutes`)
- optional daily time window is enforced (`dailyWindowStart` / `dailyWindowEnd`)
- optional duplicate guard blocks repeated check-ins (`duplicateGuardEnabled`)
  - per-day mode (`singleCheckInPerDay`)
  - interval mode (`minMinutesBetweenCheckIns`)
- daily window and per-day duplicate checks are evaluated in the site timezone

Main env vars:

- `APP_ATTENDANCE_RULES_MAX_RETROACTIVE_MINUTES`
- `APP_ATTENDANCE_RULES_MAX_FUTURE_TOLERANCE_SECONDS`
- `APP_ATTENDANCE_RULES_DUPLICATE_GUARD_ENABLED`
- `APP_ATTENDANCE_RULES_SINGLE_CHECK_IN_PER_DAY`
- `APP_ATTENDANCE_RULES_MIN_MINUTES_BETWEEN_CHECK_INS`
- `APP_ATTENDANCE_RULES_ENFORCE_DAILY_WINDOW`
- `APP_ATTENDANCE_RULES_DAILY_WINDOW_START`
- `APP_ATTENDANCE_RULES_DAILY_WINDOW_END`

## Attendance self-checkin authorization

- `EMPLOYEE` users can only check in for themselves.
- `ADMIN` and `HR` can check in for any employee.
- Self identity is resolved from JWT claims in this order:
  - `employeeId` / `employee_id`
  - `employeeCode` / `employee_code`
  - `email`
  - `preferred_username`
  - `sub` (subject)

## Attendance idempotency

- `POST /api/attendance/check-in` supports optional `Idempotency-Key` header.
- Reusing the same key with the same request returns the original response.
- Reusing the same key with different request payload is rejected (`409 Conflict`).

## Attendance audit trail

- Corrections and cancellations are now audited with:
  - who (`actorSubject`, `actorEmployeeId`, `actorRoles`)
  - when (`createdAt`)
  - why (`reason`)
- New endpoints:
  - `PUT /api/attendance/{id}/correction`
  - `POST /api/attendance/{id}/cancel`
  - `GET /api/attendance/{id}/audit`
- Correction/cancellation operations require a non-empty `reason`.
- Access policy: only `ADMIN` and `HR` can correct, cancel, and view audit trail.

## Security model

- This API is a JWT resource server.
- It validates JWT signature, `iss` (issuer), and `aud` (audience) claims.
- It expects a Bearer token containing claim `roles` (`ADMIN`, `HR`, `EMPLOYEE`).
- Authentication and token issuing are handled by the separate auth microservice.
- CORS is allowlist-based and validated at startup.
- In secure CORS mode (`prod`), origins must be HTTPS and localhost origins are rejected.
- In `prod`, Swagger/API docs and `h2-console` are denied by security rules.
- Public actuator access is limited to health endpoints.

## Profiles

- `dev` (default): H2, Swagger enabled, Flyway enabled, local fallback secrets
- `test`: H2, Flyway enabled, deterministic test secrets
- `prod`: PostgreSQL, Flyway enabled, Swagger disabled, secrets/env required

## Run attendance API (local dev)

```powershell
.\mvnw.cmd spring-boot:run
```

## Run full local stack (with external auth image)

1. Build auth image from dedicated auth repo:

```powershell
cd C:\Users\User\Documents\DEV\DEV-FREE\cale-auth-service
docker build -t cale-auth-service:latest .
```

2. Start attendance stack from this repo:

```powershell
cd C:\Users\User\Documents\DEV\DEV-FREE\attendance-app-api
# first time: copy .env.example to .env and set secure values
docker compose up --build -d
```

The compose file starts:

- `attendance-db` (PostgreSQL)
- `auth-db` (PostgreSQL)
- `cale-auth-service` (from external image)
- `attendance-app-api`

## Build and test

```powershell
.\mvnw.cmd test
```

Flyway migrations are versioned by database vendor:

- `src/main/resources/db/migration/postgresql`
- `src/main/resources/db/migration/h2`

## Important env vars

- `APP_SECURITY_JWT_SECRET` (required in `prod`, must match auth service signing secret)
- `APP_SECURITY_JWT_ISSUER` (must match `APP_JWT_ISSUER` from auth service)
- `APP_SECURITY_JWT_AUDIENCE` (must match `APP_JWT_AUDIENCE` from auth service)
- `APP_SECURITY_CORS_ALLOWED_ORIGINS` (required in `prod`, comma-separated origins)
- `APP_SECURITY_CORS_ALLOWED_METHODS`
- `APP_SECURITY_CORS_ALLOWED_HEADERS`
- `APP_SECURITY_CORS_EXPOSED_HEADERS`
- `APP_SECURITY_CORS_ALLOW_CREDENTIALS`
- `APP_SECURITY_CORS_MAX_AGE_SECONDS`
- `APP_SECURITY_CORS_ENFORCE_SECURE_POLICY` (`true` recommended in prod)
- `SPRING_PROFILES_ACTIVE` (`dev` or `prod`)
- `SPRING_DATASOURCE_*`

For Docker Compose local runs, use `.env.example` as template and create a local `.env` file.
