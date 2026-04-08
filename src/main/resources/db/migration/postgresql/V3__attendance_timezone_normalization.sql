ALTER TABLE company_sites
    ADD COLUMN timezone VARCHAR(64);

UPDATE company_sites
SET timezone = 'UTC'
WHERE timezone IS NULL OR BTRIM(timezone) = '';

ALTER TABLE company_sites
    ALTER COLUMN timezone SET DEFAULT 'UTC';

ALTER TABLE company_sites
    ALTER COLUMN timezone SET NOT NULL;

ALTER TABLE attendance_records
    ALTER COLUMN arrival_time TYPE TIMESTAMP WITH TIME ZONE
    USING (arrival_time AT TIME ZONE 'UTC');

ALTER TABLE attendance_records
    ALTER COLUMN recorded_at TYPE TIMESTAMP WITH TIME ZONE
    USING (recorded_at AT TIME ZONE 'UTC');

ALTER TABLE attendance_idempotency_keys
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
    USING (created_at AT TIME ZONE 'UTC');
