package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Room;
import com.hostelsync.backend.entity.RoomFurnitureHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomFurnitureHistoryRepository extends JpaRepository<RoomFurnitureHistory, Long> {

    List<RoomFurnitureHistory> findByRoomOrderByRecordedAtDesc(Room room);

    List<RoomFurnitureHistory> findByRoomIdOrderByRecordedAtDesc(Long roomId);
}
