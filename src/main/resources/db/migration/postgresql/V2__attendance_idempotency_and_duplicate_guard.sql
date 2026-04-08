CREATE TABLE attendance_idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    employee_id BIGINT NOT NULL,
    attendance_record_id BIGINT NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE attendance_idempotency_keys
    ADD CONSTRAINT uk_attendance_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE attendance_idempotency_keys
    ADD CONSTRAINT fk_attendance_idempotency_employee
    FOREIGN KEY (employee_id) REFERENCES employees(id);

ALTER TABLE attendance_idempotency_keys
    ADD CONSTRAINT fk_attendance_idempotency_record
    FOREIGN KEY (attendance_record_id) REFERENCES attendance_records(id);

CREATE INDEX idx_attendance_idempotency_employee_id
    ON attendance_idempotency_keys (employee_id);

CREATE INDEX idx_attendance_idempotency_record_id
    ON attendance_idempotency_keys (attendance_record_id);
