package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Hostel;
import com.hostelsync.backend.entity.Room;
import com.hostelsync.backend.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHostel(Hostel hostel);

    Optional<Room> findByRoomNumberAndHostel(String roomNumber, Hostel hostel);

    boolean existsByRoomNumberAndHostel(String roomNumber, Hostel hostel);

    List<Room> findByActiveTrue();

    /**
     * Find rooms where occupant count is less than capacity.
     * Uses a subquery counting Active students assigned to each room.
     */
    @Query("""
           SELECT r FROM Room r
           WHERE r.active = true
           AND (SELECT COUNT(s) FROM Student s WHERE s.room = r AND s.status = :activeStatus)
               < r.capacity
           """)
    List<Room> findAvailableRooms(@Param("activeStatus") StudentStatus activeStatus);

    /**
     * Count active students in a room (used to build occupancy number in responses).
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.room = :room AND s.status = :status")
    long countActiveStudentsInRoom(@Param("room") Room room, @Param("status") StudentStatus status);

    /** Total active beds across all rooms */
    @Query("SELECT COALESCE(SUM(r.capacity), 0) FROM Room r WHERE r.active = true")
    long sumCapacity();

    /** Count rooms */
    long countByActiveTrue();
}
