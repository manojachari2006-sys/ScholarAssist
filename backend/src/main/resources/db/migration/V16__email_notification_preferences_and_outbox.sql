ALTER TABLE users
    ADD COLUMN email_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE notification_email_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    notification_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    next_attempt_at DATETIME NOT NULL,
    sent_at DATETIME NULL,
    last_error VARCHAR(120) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_email_outbox_notification UNIQUE (notification_id),
    CONSTRAINT fk_email_outbox_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_email_outbox_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    INDEX idx_email_outbox_due (status, next_attempt_at, created_at)
);
