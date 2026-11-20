package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.ApplicationSupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationSupportRequestRepository
        extends JpaRepository<ApplicationSupportRequest, Long> {
    List<ApplicationSupportRequest> findByApplication_IdOrderByRequestedAtDescIdDesc(Long applicationId);
    List<ApplicationSupportRequest> findByStatusOrderByRequestedAtAscIdAsc(String status);
    boolean existsByApplication_IdAndStatus(Long applicationId, String status);
}
