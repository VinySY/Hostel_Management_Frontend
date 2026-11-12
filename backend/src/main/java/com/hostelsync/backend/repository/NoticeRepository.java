package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("""
           SELECT n FROM Notice n
           WHERE n.active = true
           AND (n.expiresAt IS NULL OR n.expiresAt > :now)
           AND (n.hostel IS NULL OR n.hostel.id = :hostelId)
           ORDER BY n.createdAt DESC
           """)
    List<Notice> findActiveNoticesForHostel(@Param("hostelId") Long hostelId, @Param("now") LocalDateTime now);

    @Query("""
           SELECT n FROM Notice n
           WHERE n.active = true
           AND (n.expiresAt IS NULL OR n.expiresAt > :now)
           ORDER BY n.createdAt DESC
           """)
    List<Notice> findAllActiveNotices(@Param("now") LocalDateTime now);

    List<Notice> findAllByOrderByCreatedAtDesc();
}
