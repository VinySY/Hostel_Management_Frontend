package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.id = :notificationId AND n.readAt IS NULL")
    int markAsRead(@Param("notificationId") Long notificationId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
