package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.ScholarshipRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Database access for scholarship eligibility criteria. */
public interface ScholarshipRuleRepository extends JpaRepository<ScholarshipRule, Long> {
    /** Retrieves criteria to evaluate for one scholarship. */
    List<ScholarshipRule> findByScholarship_Id(Long scholarshipId);

    /** Replaces saved criteria when a scholarship rule list is supplied. */
    void deleteByScholarship_Id(Long scholarshipId);
}
