package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.FurnitureType;
import com.hostelsync.backend.entity.Room;
import com.hostelsync.backend.entity.RoomFurniture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomFurnitureRepository extends JpaRepository<RoomFurniture, Long> {

    List<RoomFurniture> findByRoom(Room room);

    List<RoomFurniture> findByRoomId(Long roomId);

    Optional<RoomFurniture> findByRoomAndFurnitureType(Room room, FurnitureType furnitureType);

    Optional<RoomFurniture> findByRoomIdAndFurnitureType(Long roomId, FurnitureType furnitureType);
}
