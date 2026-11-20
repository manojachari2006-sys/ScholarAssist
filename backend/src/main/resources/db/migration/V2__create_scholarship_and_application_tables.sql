-- ============================================
-- SCHOLARSHIPS
-- ============================================

CREATE TABLE scholarships (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,

                              name VARCHAR(200) NOT NULL,
                              provider VARCHAR(200) NOT NULL,
                              description TEXT,

                              amount DECIMAL(12,2),
                              deadline DATE,

                              education_level VARCHAR(100),
                              state VARCHAR(100),
                              category VARCHAR(50),

                              status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP
);


-- ============================================
-- SCHOLARSHIP RULES
-- ============================================

CREATE TABLE scholarship_rules (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                   scholarship_id BIGINT NOT NULL,

                                   rule_type VARCHAR(50) NOT NULL,
                                   operator VARCHAR(10) NOT NULL,
                                   rule_value VARCHAR(255) NOT NULL,
                                   description VARCHAR(500),

                                   CONSTRAINT fk_rule_scholarship
                                       FOREIGN KEY (scholarship_id)
                                           REFERENCES scholarships(id)
                                           ON DELETE CASCADE
);


-- ============================================
-- ELIGIBILITY RESULTS
-- ============================================

CREATE TABLE eligibility_results (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                     student_id BIGINT NOT NULL,
                                     scholarship_id BIGINT NOT NULL,

                                     status VARCHAR(30) NOT NULL,
                                     reason TEXT,

                                     evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_eligibility_student
                                         FOREIGN KEY (student_id)
                                             REFERENCES student_profiles(id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT fk_eligibility_scholarship
                                         FOREIGN KEY (scholarship_id)
                                             REFERENCES scholarships(id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT uq_student_scholarship
                                         UNIQUE (student_id, scholarship_id)
);


-- ============================================
-- APPLICATIONS
-- ============================================

CREATE TABLE applications (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,

                              student_id BIGINT NOT NULL,
                              scholarship_id BIGINT NOT NULL,

                              application_date DATE NOT NULL,

                              status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',

                              remarks TEXT,

                              submitted_at TIMESTAMP NULL,

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,

                              CONSTRAINT fk_application_student
                                  FOREIGN KEY (student_id)
                                      REFERENCES student_profiles(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_application_scholarship
                                  FOREIGN KEY (scholarship_id)
                                      REFERENCES scholarships(id)
                                      ON DELETE CASCADE
);


-- ============================================
-- DOCUMENTS
-- ============================================

CREATE TABLE documents (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,

                           student_id BIGINT NOT NULL,

                           document_type VARCHAR(100) NOT NULL,
                           file_name VARCHAR(255) NOT NULL,
                           file_path VARCHAR(500) NOT NULL,

                           verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                           uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_document_student
                               FOREIGN KEY (student_id)
                                   REFERENCES student_profiles(id)
                                   ON DELETE CASCADE
);


-- ============================================
-- APPLICATION DOCUMENTS
-- ============================================

CREATE TABLE application_documents (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                       application_id BIGINT NOT NULL,
                                       document_id BIGINT NOT NULL,

                                       required BOOLEAN NOT NULL DEFAULT TRUE,
                                       submitted BOOLEAN NOT NULL DEFAULT FALSE,

                                       CONSTRAINT fk_app_document_application
                                           FOREIGN KEY (application_id)
                                               REFERENCES applications(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT fk_app_document_document
                                           FOREIGN KEY (document_id)
                                               REFERENCES documents(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT uq_application_document
                                           UNIQUE (application_id, document_id)
);


-- ============================================
-- NOTIFICATIONS
-- ============================================

CREATE TABLE notifications (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,

                               user_id BIGINT NOT NULL,

                               title VARCHAR(200) NOT NULL,
                               message TEXT NOT NULL,

                               type VARCHAR(50) NOT NULL,

                               is_read BOOLEAN NOT NULL DEFAULT FALSE,

                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_notification_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE
);