package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Database access for user notifications. */
public interface NoticationRepository extends JpaRepository<Notification, Long> {
    /** Returns all user notifications from newest to oldest. */
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /** Returns unread user notifications from newest to oldest. */
    List<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}
