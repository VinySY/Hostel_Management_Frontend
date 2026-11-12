package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.ComplaintEscalation;
import com.hostelsync.backend.entity.EscalationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintEscalationRepository extends JpaRepository<ComplaintEscalation, Long> {
    List<ComplaintEscalation> findByComplaintIdOrderByEscalatedAtDesc(Long complaintId);
    List<ComplaintEscalation> findByStatusOrderByEscalatedAtDesc(EscalationStatus status);
    Optional<ComplaintEscalation> findFirstByComplaintIdOrderByEscalatedAtDesc(Long complaintId);
    List<ComplaintEscalation> findByHandledByDgmIdOrderByEscalatedAtDesc(Long dgmId);

    /** All escalations from complaints in the specified hostels */
    @Query("""
           SELECT e FROM ComplaintEscalation e
           JOIN e.complaint c
           JOIN c.student s
           JOIN s.room r
           JOIN r.hostel h
           WHERE h.id IN :hostelIds
           ORDER BY e.escalatedAt DESC
           """)
    List<ComplaintEscalation> findByHostelIds(@Param("hostelIds") List<Long> hostelIds);
}
