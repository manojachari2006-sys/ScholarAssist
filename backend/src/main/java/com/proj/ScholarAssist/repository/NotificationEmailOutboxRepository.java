package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.NotificationEmailOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Database access for queued email delivery attempts. */
public interface NotificationEmailOutboxRepository extends JpaRepository<NotificationEmailOutbox, Long> {
    Optional<NotificationEmailOutbox> findByNotification_Id(Long notificationId);

    @Query("select item.id from NotificationEmailOutbox item where item.status = 'PENDING' and item.nextAttemptAt <= :now order by item.createdAt, item.id")
    List<Long> findDueIds(@Param("now") LocalDateTime now,
                          org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from NotificationEmailOutbox item where item.id = :id")
    Optional<NotificationEmailOutbox> findByIdForUpdate(@Param("id") Long id);
}
