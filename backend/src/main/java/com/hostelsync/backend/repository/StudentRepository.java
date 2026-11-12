package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Room;
import com.hostelsync.backend.entity.Student;
import com.hostelsync.backend.entity.StudentStatus;
import com.hostelsync.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentId(String studentId);

    Optional<Student> findByUser(User user);

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByUserEmail(String email);

    boolean existsByStudentId(String studentId);

    boolean existsByUserEmail(String email);

    List<Student> findAllByOrderByCreatedAtDesc();

    List<Student> findByRoom(Room room);

    @Query("SELECT s FROM Student s WHERE s.room.hostel.id = :hostelId AND s.status = :status")
    List<Student> findByRoomHostelIdAndStatus(@Param("hostelId") Long hostelId, @Param("status") StudentStatus status);

    /** Count active students in a given room (for capacity enforcement) */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.room = :room AND s.status = :status")
    long countByRoomAndStatus(@Param("room") Room room, @Param("status") StudentStatus status);

    long countByStatus(StudentStatus status);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.room.hostel.id = :hostelId AND s.status = :status")
    long countByRoomHostelIdAndStatus(@Param("hostelId") Long hostelId, @Param("status") StudentStatus status);
}

