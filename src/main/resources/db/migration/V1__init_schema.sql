CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE departments
    ADD CONSTRAINT uk_department_code UNIQUE (code);

ALTER TABLE departments
    ADD CONSTRAINT uk_department_name UNIQUE (name);

CREATE TABLE job_positions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE job_positions
    ADD CONSTRAINT uk_job_position_code UNIQUE (code);

ALTER TABLE job_positions
    ADD CONSTRAINT uk_job_position_name UNIQUE (name);

CREATE TABLE company_sites (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    geofence_radius_meters INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE company_sites
    ADD CONSTRAINT uk_site_code UNIQUE (code);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    position_id BIGINT,
    department_id BIGINT,
    hire_date DATE,
    birth_date DATE,
    birth_place VARCHAR(150),
    contract_type VARCHAR(30),
    employee_type VARCHAR(20),
    marital_status VARCHAR(20),
    gender VARCHAR(20),
    city_of_residence VARCHAR(120),
    district VARCHAR(120),
    nationality VARCHAR(80),
    national_id_number VARCHAR(60),
    phone_number VARCHAR(30),
    address VARCHAR(255),
    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(30),
    contract_start_date DATE,
    contract_end_date DATE,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE employees
    ADD CONSTRAINT uk_employee_code UNIQUE (employee_code);

ALTER TABLE employees
    ADD CONSTRAINT uk_employee_email UNIQUE (email);

ALTER TABLE employees
    ADD CONSTRAINT fk_employee_position FOREIGN KEY (position_id) REFERENCES job_positions(id);

ALTER TABLE employees
    ADD CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES departments(id);

CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    check_in_source VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    distance_meters DOUBLE PRECISION,
    notes VARCHAR(500),
    recorded_at TIMESTAMP NOT NULL
);

ALTER TABLE attendance_records
    ADD CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE attendance_records
    ADD CONSTRAINT fk_attendance_site FOREIGN KEY (site_id) REFERENCES company_sites(id);

CREATE INDEX idx_attendance_records_arrival_time ON attendance_records (arrival_time DESC);
CREATE INDEX idx_attendance_records_employee_id ON attendance_records (employee_id);
CREATE INDEX idx_attendance_records_site_id ON attendance_records (site_id);