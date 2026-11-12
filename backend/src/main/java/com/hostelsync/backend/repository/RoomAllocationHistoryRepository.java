package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.RoomAllocationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomAllocationHistoryRepository extends JpaRepository<RoomAllocationHistory, Long> {
    Page<RoomAllocationHistory> findByStudentIdOrderByActionTimestampDesc(Long studentId, Pageable pageable);
    Page<RoomAllocationHistory> findByRoomIdOrderByActionTimestampDesc(Long roomId, Pageable pageable);
    List<RoomAllocationHistory> findByPerformedByIdOrderByActionTimestampDesc(Long performedById);

    @Query("SELECT rah FROM RoomAllocationHistory rah WHERE rah.actionTimestamp >= :from AND rah.actionTimestamp <= :to ORDER BY rah.actionTimestamp DESC")
    Page<RoomAllocationHistory> findByTimestampRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
