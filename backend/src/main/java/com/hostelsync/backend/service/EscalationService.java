package com.hostelsync.backend.service;

import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.ComplaintEscalationRepository;
import com.hostelsync.backend.repository.ComplaintRepository;
import com.hostelsync.backend.repository.DgmRepository;
import com.hostelsync.backend.repository.HostelDgmAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EscalationService {

    private final ComplaintEscalationRepository escalationRepository;
    private final ComplaintRepository complaintRepository;
    private final DgmRepository dgmRepository;
    private final HostelDgmAssignmentRepository hostelDgmAssignmentRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SlaService slaService;

    public EscalationService(ComplaintEscalationRepository escalationRepository,
                            ComplaintRepository complaintRepository,
                            DgmRepository dgmRepository,
                            HostelDgmAssignmentRepository hostelDgmAssignmentRepository,
                            NotificationService notificationService,
                            AuditService auditService,
                            SlaService slaService) {
        this.escalationRepository = escalationRepository;
        this.complaintRepository = complaintRepository;
        this.dgmRepository = dgmRepository;
        this.hostelDgmAssignmentRepository = hostelDgmAssignmentRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.slaService = slaService;
    }

    /**
     * Escalate a complaint to DGM (student-initiated).
     */
    public ComplaintEscalation escalateComplaint(Long complaintId, Long studentUserId,
                                                 String reason, String supportingDetails) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", complaintId));

        // Verify the student owns this complaint
        if (!complaint.getStudent().getUser().getId().equals(studentUserId)) {
            throw new ForbiddenException("You can only escalate your own complaints");
        }

        // Check if complaint is overdue (required for student-initiated escalation)
        if (!slaService.isOverdue(complaint.getSlaDeadline())) {
            throw new BadRequestException("Complaint can only be escalated if it is overdue");
        }

        // Check if already escalated
        if (complaint.isEscalated()) {
            throw new BadRequestException("Complaint is already escalated");
        }

        // Check if complaint is resolved
        if (complaint.getStatus() == ComplaintStatus.Resolved ||
            complaint.getStatus() == ComplaintStatus.Rejected) {
            throw new BadRequestException("Cannot escalate a resolved or rejected complaint");
        }

        // Create escalation record
        ComplaintEscalation escalation = new ComplaintEscalation();
        escalation.setComplaint(complaint);
        escalation.setEscalatedByStudent(complaint.getStudent());
        escalation.setReason(reason);
        escalation.setSupportingDetails(supportingDetails);
        escalation.setStatus(EscalationStatus.PENDING);
        escalation = escalationRepository.save(escalation);

        // Update complaint
        complaint.setEscalated(true);
        complaint.setEscalatedAt(LocalDateTime.now());
        complaint.getTimeline().add(new ComplaintTimeline(
            complaint,
            complaint.getStatus(),
            "Escalated to DGM by student: " + reason
        ));
        complaintRepository.save(complaint);

        // Notify DGMs responsible for this hostel
        Student student = complaint.getStudent();
        if (student.getRoom() != null && student.getRoom().getHostel() != null) {
            Long hostelId = student.getRoom().getHostel().getId();
            // Get DGMs assigned to this hostel and notify them
            List<HostelDgmAssignment> assignments = hostelDgmAssignmentRepository.findByHostelId(hostelId);
            for (HostelDgmAssignment assignment : assignments) {
                Dgm dgm = assignment.getDgm();
                notificationService.notifyComplaintEscalated(
                    dgm.getUser().getId(),
                    complaintId,
                    complaint.getComplaintNumber()
                );
            }
        }

        // Audit log
        auditService.logComplaintEscalation(
            complaintId,
            complaint.getStudent().getUser(),
            reason
        );

        return escalation;
    }

    /**
     * Initiate executive escalation on a complaint directly as an authorized DGM.
     */
    public ComplaintEscalation escalateComplaintByDgm(Long complaintId, Long dgmUserId,
                                                      String reason, String supportingDetails) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", complaintId));

        Dgm dgm = dgmRepository.findByUserId(dgmUserId)
                .orElseThrow(() -> new ResourceNotFoundException("DGM", dgmUserId));

        Student student = complaint.getStudent();
        if (student.getRoom() == null || student.getRoom().getHostel() == null) {
            throw new ForbiddenException("Cannot determine complaint hostel for DGM authorization");
        }

        Long hostelId = student.getRoom().getHostel().getId();
        boolean hasAccess = hostelDgmAssignmentRepository
                .findByHostelIdAndDgmId(hostelId, dgm.getId())
                .isPresent();
        if (!hasAccess) {
            throw new ForbiddenException("You are not authorized to escalate complaints for this hostel");
        }

        if (complaint.isEscalated()) {
            throw new BadRequestException("Complaint is already escalated");
        }

        if (complaint.getStatus() == ComplaintStatus.Resolved ||
            complaint.getStatus() == ComplaintStatus.Rejected) {
            throw new BadRequestException("Cannot escalate a resolved or rejected complaint");
        }

        ComplaintEscalation escalation = new ComplaintEscalation();
        escalation.setComplaint(complaint);
        escalation.setEscalatedByStudent(student);
        escalation.setReason(reason);
        escalation.setSupportingDetails(supportingDetails);
        escalation.setStatus(EscalationStatus.PENDING);
        escalation = escalationRepository.save(escalation);

        complaint.setEscalated(true);
        complaint.setEscalatedAt(LocalDateTime.now());
        complaint.getTimeline().add(new ComplaintTimeline(
            complaint,
            complaint.getStatus(),
            "Executive escalation initiated by DGM: " + reason
        ));
        complaintRepository.save(complaint);

        auditService.logComplaintEscalation(
            complaintId,
            dgm.getUser(),
            reason
        );

        if (complaint.getAssignedWarden() != null) {
            notificationService.notifyDgmActionTaken(
                complaint.getAssignedWarden().getUser().getId(),
                complaint.getId(),
                complaint.getComplaintNumber(),
                "Executive escalation initiated: " + reason
            );
        }

        return escalation;
    }

    /**
     * Handle escalation as DGM.
     */
    public ComplaintEscalation handleEscalation(Long escalationId, Long dgmUserId,
                                                String dgmResponse, EscalationStatus newStatus) {
        ComplaintEscalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new ResourceNotFoundException("Escalation", escalationId));

        Dgm dgm = dgmRepository.findByUserId(dgmUserId)
                .orElseThrow(() -> new ResourceNotFoundException("DGM", dgmUserId));

        // Verify DGM has access to this hostel - fail closed on missing relationship
        Complaint complaint = escalation.getComplaint();
        Student student = complaint.getStudent();

        if (student.getRoom() == null) {
            throw new ForbiddenException("Student has no room assignment - cannot determine hostel for authorization");
        }

        if (student.getRoom().getHostel() == null) {
            throw new ForbiddenException("Room has no hostel assignment - cannot authorize DGM access");
        }

        Long hostelId = student.getRoom().getHostel().getId();
        boolean hasAccess = hostelDgmAssignmentRepository
                .findByHostelIdAndDgmId(hostelId, dgm.getId())
                .isPresent();

        if (!hasAccess) {
            throw new ForbiddenException("You are not authorized to handle escalations for this hostel");
        }

        if (escalation.getStatus() != EscalationStatus.PENDING &&
            escalation.getStatus() != EscalationStatus.IN_REVIEW) {
            throw new BadRequestException("This escalation has already been handled");
        }

        escalation.setDgmResponse(dgmResponse);
        escalation.setHandledByDgmId(dgm.getId());
        escalation.setHandledAt(LocalDateTime.now());
        escalation.setStatus(newStatus);
        escalation = escalationRepository.save(escalation);

        complaint.getTimeline().add(new ComplaintTimeline(
            complaint,
            complaint.getStatus(),
            "DGM response: " + dgmResponse
        ));
        complaintRepository.save(complaint);

        // Notify student
        notificationService.create(
            complaint.getStudent().getUser().getId(),
            NotificationType.DGM_ACTION_TAKEN,
            "DGM has responded to your escalation for complaint " + complaint.getComplaintNumber(),
            "Complaint",
            complaint.getId()
        );

        // Notify warden if assigned
        if (complaint.getAssignedWarden() != null) {
            notificationService.notifyDgmActionTaken(
                complaint.getAssignedWarden().getUser().getId(),
                complaint.getId(),
                complaint.getComplaintNumber(),
                dgmResponse
            );
        }

        return escalation;
    }

    /**
     * Get escalation history for a complaint.
     */
    @Transactional(readOnly = true)
    public List<ComplaintEscalation> getEscalationHistory(Long complaintId) {
        return escalationRepository.findByComplaintIdOrderByEscalatedAtDesc(complaintId);
    }

    /**
     * Get pending escalations.
     */
    @Transactional(readOnly = true)
    public List<ComplaintEscalation> getPendingEscalations() {
        return escalationRepository.findByStatusOrderByEscalatedAtDesc(EscalationStatus.PENDING);
    }

    /**
     * Get escalations handled by a DGM.
     */
    @Transactional(readOnly = true)
    public List<ComplaintEscalation> getEscalationsHandledByDgm(Long dgmId) {
        return escalationRepository.findByHandledByDgmIdOrderByEscalatedAtDesc(dgmId);
    }

    /**
     * Get current escalation for a complaint.
     */
    @Transactional(readOnly = true)
    public ComplaintEscalation getCurrentEscalation(Long complaintId) {
        return escalationRepository.findFirstByComplaintIdOrderByEscalatedAtDesc(complaintId)
                .orElse(null);
    }

    /**
     * Get escalation by its primary key ID.
     */
    @Transactional(readOnly = true)
    public Optional<ComplaintEscalation> getEscalationById(Long escalationId) {
        return escalationRepository.findById(escalationId);
    }
}
