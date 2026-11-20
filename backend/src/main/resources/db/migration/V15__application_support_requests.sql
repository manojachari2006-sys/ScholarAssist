CREATE TABLE application_support_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    request_type VARCHAR(30) NOT NULL,
    student_message VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_user_id BIGINT NULL,
    reviewer_message VARCHAR(2000) NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_support_request_application FOREIGN KEY (application_id)
        REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_support_request_student_user FOREIGN KEY (requested_by_user_id)
        REFERENCES users(id),
    CONSTRAINT fk_support_request_reviewer_user FOREIGN KEY (reviewer_user_id)
        REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_support_request_status_time (status, requested_at),
    INDEX idx_support_request_application_time (application_id, requested_at)
);
