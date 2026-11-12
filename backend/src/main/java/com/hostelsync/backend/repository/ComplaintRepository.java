package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Complaint;
import com.hostelsync.backend.entity.ComplaintStatus;
import com.hostelsync.backend.entity.ComplaintType;
import com.hostelsync.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByComplaintNumber(String complaintNumber);

    /** All complaints for a student, newest first */
    List<Complaint> findByStudentOrderByCreatedAtDesc(Student student);

    /** All complaints by status */
    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    /** All complaints by type */
    List<Complaint> findByTypeOrderByCreatedAtDesc(ComplaintType type);

    /** All complaints, newest first (warden view) */
    List<Complaint> findAllByOrderByCreatedAtDesc();

    /** Count open complaints (not resolved or rejected) */
    @Query("""
           SELECT COUNT(c) FROM Complaint c
           WHERE c.status NOT IN (
               com.hostelsync.backend.entity.ComplaintStatus.Resolved,
               com.hostelsync.backend.entity.ComplaintStatus.Rejected
           )
           """)
    long countOpenComplaints();

    long countByStatus(ComplaintStatus status);

    long countByType(ComplaintType type);

    /** Highest complaint number sequence (used to generate the next number) */
    @Query("SELECT MAX(c.id) FROM Complaint c")
    Optional<Long> findMaxId();

    /** All complaints in the specified hostels */
    @Query("""
           SELECT c FROM Complaint c
           JOIN c.student s
           JOIN s.room r
           JOIN r.hostel h
           WHERE h.id IN :hostelIds
           ORDER BY c.createdAt DESC
           """)
    List<Complaint> findByHostelIds(@Param("hostelIds") List<Long> hostelIds);

    @Query("""
           SELECT COUNT(c) FROM Complaint c
           WHERE c.assignedWarden.id = :wardenId
           AND c.status NOT IN (
               com.hostelsync.backend.entity.ComplaintStatus.Resolved,
               com.hostelsync.backend.entity.ComplaintStatus.Rejected
           )
           """)
    long countActiveComplaintsForWarden(@Param("wardenId") Long wardenId);

    @Query("""
           SELECT COUNT(c) FROM Complaint c
           WHERE c.assignedWarden.id = :wardenId
           AND c.status = com.hostelsync.backend.entity.ComplaintStatus.Resolved
           """)
    long countResolvedComplaintsForWarden(@Param("wardenId") Long wardenId);

    @Query("""
           SELECT COUNT(c) FROM Complaint c
           JOIN c.student s
           JOIN s.room r
           WHERE r.hostel.id = :hostelId
           AND c.status NOT IN (
               com.hostelsync.backend.entity.ComplaintStatus.Resolved,
               com.hostelsync.backend.entity.ComplaintStatus.Rejected
           )
           """)
    long countActiveComplaintsForHostel(@Param("hostelId") Long hostelId);

    @Query("""
           SELECT c FROM Complaint c
           JOIN c.student s
           JOIN s.room r
           WHERE r.hostel.id = :hostelId
           ORDER BY c.createdAt DESC
           """)
    List<Complaint> findByHostelId(@Param("hostelId") Long hostelId);

    @Query("""
           SELECT c FROM Complaint c
           WHERE (c.overdueAt IS NOT NULL OR (c.slaDeadline IS NOT NULL AND c.slaDeadline < :now))
           AND c.status NOT IN (
               com.hostelsync.backend.entity.ComplaintStatus.Resolved,
               com.hostelsync.backend.entity.ComplaintStatus.Rejected
           )
           ORDER BY c.slaDeadline ASC
           """)
    List<Complaint> findOverdueComplaints(@Param("now") java.time.LocalDateTime now);
}
