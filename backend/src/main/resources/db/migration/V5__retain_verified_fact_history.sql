ALTER TABLE student_verified_facts
    DROP INDEX uk_verified_fact_student_type,
    ADD INDEX idx_verified_fact_student_type_time (student_id, fact_type, verified_at);
