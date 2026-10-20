package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Complaint;
import com.hostelsync.backend.entity.ComplaintAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintAttachmentRepository extends JpaRepository<ComplaintAttachment, Long> {

    List<ComplaintAttachment> findByComplaint(Complaint complaint);

    Optional<ComplaintAttachment> findByStoredFileName(String storedFileName);
}
