package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.EligibilityResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** Database access for saved student/scholarship eligibility decisions. */
public interface EligibilityRepository extends JpaRepository<EligibilityResult, Long> {
    /** Looks up the unique result row that evaluation should update. */
    Optional<EligibilityResult> findByStudent_IdAndScholarship_Id(
            Long studentId,
            Long scholarshipId);

    /** Lists saved decisions for one student profile. */
    List<EligibilityResult> findByStudent_Id(Long studentId);
}
