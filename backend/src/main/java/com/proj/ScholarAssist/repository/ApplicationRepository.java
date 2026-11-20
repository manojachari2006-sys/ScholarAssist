package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/** Database access for scholarship applications. */
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    /** Lists applications owned by a student profile. */
    List<Application> findByStudent_Id(Long studentId);

    /** Finds the student's existing tracker record without creating duplicates. */
    Optional<Application> findByStudent_IdAndScholarship_Id(Long studentId, Long scholarshipId);

    /** Supplies reviewers with the most recently updated applications first. */
    List<Application> findAllByOrderByUpdatedAtDesc();

    /** Loads only applications that currently need reviewer attention. */
    List<Application> findByStatusInOrderByUpdatedAtDesc(List<String> statuses);

    /** Serializes submission confirmations and reviewer updates for one application. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id")
    Optional<Application> findByIdForUpdate(@Param("id") Long id);

    /** Supports the project rule that a student may apply once per scholarship. */
    boolean existsByStudent_IdAndScholarship_Id(Long studentId, Long scholarshipId);
}
