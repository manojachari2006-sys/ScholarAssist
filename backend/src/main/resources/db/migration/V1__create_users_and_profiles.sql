CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,

                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,

                       role VARCHAR(50) NOT NULL DEFAULT 'STUDENT',

                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP
);


CREATE TABLE student_profiles (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                  user_id BIGINT NOT NULL UNIQUE,

                                  full_name VARCHAR(150) NOT NULL,
                                  phone VARCHAR(20),

                                  date_of_birth DATE,
                                  gender VARCHAR(30),

                                  state VARCHAR(100),
                                  district VARCHAR(100),

                                  college_name VARCHAR(200),
                                  course VARCHAR(150),
                                  year_of_study INT,

                                  cgpa DECIMAL(4,2),

                                  annual_family_income DECIMAL(12,2),

                                  category VARCHAR(50),

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_student_profile_user
                                      FOREIGN KEY (user_id)
                                          REFERENCES users(id)
                                          ON DELETE CASCADE
);