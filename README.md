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

## Security model

- This API is a JWT resource server.
- It expects a Bearer token containing claim `roles` (`ADMIN`, `HR`, `EMPLOYEE`).
- Authentication and token issuing are handled by the separate auth microservice.

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
- `APP_SECURITY_CORS_ALLOWED_ORIGINS` (required in `prod`, comma-separated origins)
- `SPRING_PROFILES_ACTIVE` (`dev` or `prod`)
- `SPRING_DATASOURCE_*`

For Docker Compose local runs, use `.env.example` as template and create a local `.env` file.
