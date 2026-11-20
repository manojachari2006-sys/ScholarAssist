package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.Scholarship;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Database access for scholarships. */
public interface ScholarshipRepository extends JpaRepository<Scholarship, Long> {
    /** Searches scholarship names and providers without case sensitivity. */
    List<Scholarship> findByNameContainingIgnoreCaseOrProviderContainingIgnoreCase(
            String name,
            String provider);

    /** Retrieves records currently offered to students. */
    List<Scholarship> findByStatusIgnoreCase(String status);
}
