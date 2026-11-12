package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Hostel;
import com.hostelsync.backend.entity.Warden;
import com.hostelsync.backend.entity.WardenFloorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardenFloorAssignmentRepository extends JpaRepository<WardenFloorAssignment, Long> {

    List<WardenFloorAssignment> findByWarden(Warden warden);

    List<WardenFloorAssignment> findByWardenId(Long wardenId);

    List<WardenFloorAssignment> findByHostelId(Long hostelId);

    List<WardenFloorAssignment> findByHostelIdAndFloor(Long hostelId, int floor);

    Optional<WardenFloorAssignment> findByWardenIdAndHostelIdAndFloor(Long wardenId, Long hostelId, int floor);

    void deleteByWardenId(Long wardenId);
}
