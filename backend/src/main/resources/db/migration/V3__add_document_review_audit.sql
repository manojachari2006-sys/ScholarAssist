ALTER TABLE documents
    ADD COLUMN reviewed_at TIMESTAMP NULL,
    ADD COLUMN review_feedback TEXT NULL,
    ADD COLUMN reviewer_id BIGINT NULL,
    ADD CONSTRAINT fk_document_reviewer
        FOREIGN KEY (reviewer_id)
            REFERENCES users(id)
            ON DELETE SET NULL;
