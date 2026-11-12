package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.ComplaintAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintAssignmentRepository extends JpaRepository<ComplaintAssignment, Long> {
    List<ComplaintAssignment> findByComplaintIdOrderByAssignedAtDesc(Long complaintId);
    List<ComplaintAssignment> findByAssignedWardenIdOrderByAssignedAtDesc(Long wardenId);
    Optional<ComplaintAssignment> findFirstByComplaintIdOrderByAssignedAtDesc(Long complaintId);
}
