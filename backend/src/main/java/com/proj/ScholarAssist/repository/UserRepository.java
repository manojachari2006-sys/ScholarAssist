package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Database access for user records. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
