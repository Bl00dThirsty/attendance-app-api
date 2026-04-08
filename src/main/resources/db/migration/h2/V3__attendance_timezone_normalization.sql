ALTER TABLE company_sites
    ADD COLUMN timezone VARCHAR(64);

UPDATE company_sites
SET timezone = 'UTC'
WHERE timezone IS NULL OR TRIM(timezone) = '';

ALTER TABLE company_sites
    ALTER COLUMN timezone SET DEFAULT 'UTC';

ALTER TABLE company_sites
    ALTER COLUMN timezone SET NOT NULL;

ALTER TABLE attendance_records
    ALTER COLUMN arrival_time SET DATA TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE attendance_records
    ALTER COLUMN recorded_at SET DATA TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE attendance_idempotency_keys
    ALTER COLUMN created_at SET DATA TYPE TIMESTAMP WITH TIME ZONE;
