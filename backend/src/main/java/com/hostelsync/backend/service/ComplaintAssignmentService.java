package com.hostelsync.backend.service;

import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.ComplaintAssignmentRepository;
import com.hostelsync.backend.repository.ComplaintRepository;
import com.hostelsync.backend.repository.WardenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ComplaintAssignmentService {

    private final ComplaintAssignmentRepository assignmentRepository;
    private final ComplaintRepository complaintRepository;
    private final WardenRepository wardenRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SlaService slaService;

    public ComplaintAssignmentService(ComplaintAssignmentRepository assignmentRepository,
                                     ComplaintRepository complaintRepository,
                                     WardenRepository wardenRepository,
                                     NotificationService notificationService,
                                     AuditService auditService,
                                     SlaService slaService) {
        this.assignmentRepository = assignmentRepository;
        this.complaintRepository = complaintRepository;
        this.wardenRepository = wardenRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.slaService = slaService;
    }

    /**
     * Assign a complaint to a warden.
     */
    public ComplaintAssignment assignComplaint(Long complaintId, Long wardenId, Long assignedById, String reason) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", complaintId));

        Warden warden = wardenRepository.findById(wardenId)
                .orElseThrow(() -> new ResourceNotFoundException("Warden", wardenId));

        // Check if complaint is already assigned to this warden
        if (complaint.getAssignedWarden() != null
            && complaint.getAssignedWarden().getId().equals(wardenId)) {
            throw new BadRequestException("Complaint is already assigned to this warden");
        }

        boolean isReassignment = complaint.getAssignedWarden() != null;

        // Create assignment record
        ComplaintAssignment assignment = new ComplaintAssignment();
        assignment.setComplaint(complaint);
        assignment.setAssignedWarden(warden);
        assignment.setAssignedById(assignedById);
        assignment.setReason(reason);
        assignment = assignmentRepository.save(assignment);

        // Update complaint
        complaint.setAssignedWarden(warden);

        // Set SLA deadline if not already set
        if (complaint.getSlaDeadline() == null) {
            LocalDateTime deadline = slaService.calculateDeadline(
                complaint.getPriority(),
                LocalDateTime.now()
            );
            complaint.setSlaDeadline(deadline);
        }

        // Update status to Under_Review if still Submitted
        if (complaint.getStatus() == ComplaintStatus.Submitted) {
            complaint.setStatus(ComplaintStatus.Under_Review);
            complaint.getTimeline().add(new ComplaintTimeline(
                complaint,
                ComplaintStatus.Under_Review,
                "Assigned to warden: " + warden.getUser().getName()
            ));
        }

        complaintRepository.save(complaint);

        // Send notification
        if (isReassignment) {
            notificationService.notifyComplaintReassigned(
                warden.getUser().getId(),
                complaintId,
                complaint.getComplaintNumber()
            );
        } else {
            notificationService.notifyComplaintAssigned(
                warden.getUser().getId(),
                complaintId,
                complaint.getComplaintNumber()
            );
        }

        // Audit log
        auditService.logComplaintAssignment(
            complaintId,
            wardenId,
            warden.getUser(),
            reason != null ? reason : (isReassignment ? "Reassigned" : "Initial assignment")
        );

        return assignment;
    }

    /**
     * Get assignment history for a complaint.
     */
    @Transactional(readOnly = true)
    public List<ComplaintAssignment> getAssignmentHistory(Long complaintId) {
        return assignmentRepository.findByComplaintIdOrderByAssignedAtDesc(complaintId);
    }

    /**
     * Get complaints assigned to a warden.
     */
    @Transactional(readOnly = true)
    public List<ComplaintAssignment> getAssignmentsForWarden(Long wardenId) {
        return assignmentRepository.findByAssignedWardenIdOrderByAssignedAtDesc(wardenId);
    }

    /**
     * Get current assignment for a complaint.
     */
    @Transactional(readOnly = true)
    public ComplaintAssignment getCurrentAssignment(Long complaintId) {
        return assignmentRepository.findFirstByComplaintIdOrderByAssignedAtDesc(complaintId)
                .orElse(null);
    }
}
