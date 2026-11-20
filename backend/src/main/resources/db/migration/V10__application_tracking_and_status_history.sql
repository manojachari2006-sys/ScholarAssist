ALTER TABLE applications
    ADD COLUMN provider_application_id VARCHAR(150) NULL;

CREATE TABLE application_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    previous_status VARCHAR(50) NULL,
    new_status VARCHAR(50) NOT NULL,
    note TEXT NULL,
    change_source VARCHAR(30) NOT NULL,
    changed_by_user_id BIGINT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_application_history_application
        FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_application_history_user
        FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_application_history_application_time (application_id, changed_at)
);

-- Preserve a useful starting point for applications created before status history existed.
INSERT INTO application_status_history (
    application_id,
    previous_status,
    new_status,
    note,
    change_source,
    changed_by_user_id,
    changed_at
)
SELECT a.id,
       NULL,
       a.status,
       'Current status recorded when application history tracking was enabled.',
       'MIGRATION',
       student_profile.user_id,
       a.updated_at
FROM applications a
JOIN student_profiles student_profile ON student_profile.id = a.student_id;
