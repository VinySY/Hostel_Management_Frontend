package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.HostelDgmAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HostelDgmAssignmentRepository extends JpaRepository<HostelDgmAssignment, Long> {
    List<HostelDgmAssignment> findByDgmId(Long dgmId);
    List<HostelDgmAssignment> findByHostelId(Long hostelId);
    Optional<HostelDgmAssignment> findByHostelIdAndDgmId(Long hostelId, Long dgmId);
}
