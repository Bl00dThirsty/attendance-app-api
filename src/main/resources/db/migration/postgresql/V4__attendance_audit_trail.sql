CREATE TABLE attendance_audit_trails (
    id BIGSERIAL PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    actor_subject VARCHAR(150) NOT NULL,
    actor_employee_id BIGINT,
    actor_roles VARCHAR(250) NOT NULL,
    reason VARCHAR(500),
    details VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE attendance_audit_trails
    ADD CONSTRAINT fk_attendance_audit_record
    FOREIGN KEY (attendance_record_id) REFERENCES attendance_records(id);

CREATE INDEX idx_attendance_audit_record_id
    ON attendance_audit_trails (attendance_record_id);

CREATE INDEX idx_attendance_audit_created_at
    ON attendance_audit_trails (created_at DESC);

INSERT INTO attendance_audit_trails (
    attendance_record_id,
    action,
    actor_subject,
    actor_employee_id,
    actor_roles,
    reason,
    details,
    created_at
)
SELECT
    ar.id,
    'CREATED',
    'system-migration',
    NULL,
    'ROLE_SYSTEM',
    'Baseline audit migration',
    'Historical record imported into audit trail',
    COALESCE(ar.recorded_at, ar.arrival_time, NOW())
FROM attendance_records ar
WHERE NOT EXISTS (
    SELECT 1
    FROM attendance_audit_trails a
    WHERE a.attendance_record_id = ar.id
      AND a.action = 'CREATED'
);
