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
}
