CREATE TABLE student_verified_facts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    fact_type VARCHAR(50) NOT NULL,
    fact_value VARCHAR(255) NOT NULL,
    source_document_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    verified_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_verified_fact_student_type UNIQUE (student_id, fact_type),
    CONSTRAINT fk_verified_fact_student FOREIGN KEY (student_id)
        REFERENCES student_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_verified_fact_document FOREIGN KEY (source_document_id)
        REFERENCES documents(id),
    CONSTRAINT fk_verified_fact_reviewer FOREIGN KEY (reviewer_id)
        REFERENCES users(id)
);
