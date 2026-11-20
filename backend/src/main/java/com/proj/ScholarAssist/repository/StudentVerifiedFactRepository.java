package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.StudentVerifiedFact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for the latest reviewer-confirmed facts used in eligibility decisions. */
public interface StudentVerifiedFactRepository extends JpaRepository<StudentVerifiedFact, Long> {
    List<StudentVerifiedFact> findByStudent_IdOrderByVerifiedAtDesc(Long studentId);
}
