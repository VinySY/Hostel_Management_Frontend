package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.actionTimestamp >= :from AND al.actionTimestamp <= :to ORDER BY al.actionTimestamp DESC")
    Page<AuditLog> findByTimestampRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
