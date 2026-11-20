package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Reads the ordered status audit trail for one application. */
public interface ApplicationStatusHistoryRepository
        extends JpaRepository<ApplicationStatusHistory, Long> {
    List<ApplicationStatusHistory> findByApplication_IdOrderByChangedAtAscIdAsc(
            Long applicationId);

    java.util.Optional<ApplicationStatusHistory>
    findTopByApplication_IdAndChangeSourceOrderByChangedAtDescIdDesc(
            Long applicationId, String changeSource);

    @Query("select h from ApplicationStatusHistory h where h.application.id in :applicationIds "
            + "and h.id in (select max(latest.id) from ApplicationStatusHistory latest "
            + "where latest.application.id in :applicationIds group by latest.application.id)")
    List<ApplicationStatusHistory> findLatestForApplications(
            @Param("applicationIds") List<Long> applicationIds);
}
