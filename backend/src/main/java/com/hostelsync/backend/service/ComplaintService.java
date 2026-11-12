package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.ComplaintCreateRequest;
import com.hostelsync.backend.dto.ComplaintResponse;
import com.hostelsync.backend.dto.ComplaintStatusUpdateRequest;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.ComplaintAttachmentRepository;
import com.hostelsync.backend.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final MappingService mappingService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    /** Configurable base path/URL for file download links, defaulting to relative "/api/files" */
    private final String fileBaseUrl;
    private DemoScopeService demoScopeService;

    @Autowired
    public ComplaintService(ComplaintRepository complaintRepository,
                            ComplaintAttachmentRepository attachmentRepository,
                            FileStorageService fileStorageService,
                            MappingService mappingService,
                            NotificationService notificationService,
                            AuditService auditService,
                            @Value("${app.file-base-url:/api/files}") String fileBaseUrl) {
        this.complaintRepository = complaintRepository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.mappingService = mappingService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        String resolved = (fileBaseUrl == null || fileBaseUrl.isBlank()) ? "/api/files" : fileBaseUrl.trim();
        this.fileBaseUrl = resolved.replaceAll("/+$", "");
    }

    /** Legacy constructor preserved for unit tests */
    public ComplaintService(ComplaintRepository complaintRepository,
                            ComplaintAttachmentRepository attachmentRepository,
                            FileStorageService fileStorageService,
                            MappingService mappingService,
                            NotificationService notificationService,
                            AuditService auditService,
                            String origins,
                            int port) {
        this(complaintRepository, attachmentRepository, fileStorageService,
                mappingService, notificationService, auditService, "/api/files");
    }

    @Autowired(required = false)
    public void setDemoScopeService(DemoScopeService demoScopeService) {
        this.demoScopeService = demoScopeService;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    public ComplaintResponse createComplaint(ComplaintCreateRequest req,
                                             List<MultipartFile> files,
                                             Student student) {
        Complaint complaint = new Complaint();
        complaint.setStudent(student);
        complaint.setType(ComplaintType.fromDisplayName(req.getType()));
        complaint.setSubject(req.getSubject());
        complaint.setDescription(req.getDescription());
        complaint.setLocation(req.getLocation());
        complaint.setStatus(ComplaintStatus.Submitted);

        // Priority
        if (req.getPriority() != null && !req.getPriority().isBlank()) {
            try {
                complaint.setPriority(ComplaintPriority.valueOf(req.getPriority()));
            } catch (IllegalArgumentException e) {
                complaint.setPriority(ComplaintPriority.Medium);
            }
        } else {
            complaint.setPriority(ComplaintPriority.Medium);
        }

        // Generate a unique complaint number up front using timestamp
        // Format: CMP-YYYYMMDD-XXXX where XXXX is a random 4-digit suffix
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", (int)(Math.random() * 9000) + 1000);
        complaint.setComplaintNumber("CMP-" + timestamp + "-" + random);

        // Persist to get the ID, then update to the clean sequential CMP-XXXX format
        complaint = complaintRepository.save(complaint);
        // Now we have the real ID — update to the canonical format
        complaint.setComplaintNumber("CMP-" + String.format("%04d", complaint.getId()));
        complaint = complaintRepository.save(complaint);
        ComplaintTimeline initial = new ComplaintTimeline(
                complaint, ComplaintStatus.Submitted, "Complaint submitted by student");
        complaint.getTimeline().add(initial);

        // Store uploaded files
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String storedName = fileStorageService.store(file);
                    ComplaintAttachment attachment = new ComplaintAttachment(
                            complaint,
                            file.getOriginalFilename(),
                            storedName,
                            file.getContentType(),
                            file.getSize()
                    );
                    complaint.getAttachments().add(attachment);
                }
            }
        }

        complaint = complaintRepository.save(complaint);
        return mappingService.toComplaintResponse(complaint, fileBaseUrl);
    }

    // ─── Get all (warden) ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        List<Complaint> complaints;
        if (demoScopeService != null && demoScopeService.isCurrentCallerDemo()) {
            complaints = complaintRepository.findByHostelId(demoScopeService.requireDemoHostelId());
        } else {
            complaints = complaintRepository.findAllByOrderByCreatedAtDesc();
            if (demoScopeService != null) {
                complaints = complaints.stream()
                        .filter(demoScopeService::isComplaintAccessible)
                        .toList();
            }
        }
        return complaints.stream()
                .map(c -> mappingService.toComplaintResponse(c, fileBaseUrl))
                .toList();
    }

    // ─── Get for a specific student ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsForStudent(Student student) {
        if (demoScopeService != null) {
            demoScopeService.assertStudentAccessible(student);
        }
        return complaintRepository.findByStudentOrderByCreatedAtDesc(student)
                .stream()
                .map(c -> mappingService.toComplaintResponse(c, fileBaseUrl))
                .toList();
    }

    // ─── Get single complaint ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long id, User caller) {
        Complaint complaint = findEntityById(id);
        // Students may only view their own complaints (IDOR prevention)
        if (caller.getRole() == UserRole.STUDENT
                && !complaint.getStudent().getUser().getId().equals(caller.getId())) {
            throw new ForbiddenException("You are not authorised to view this complaint.");
        }
        if (demoScopeService != null) {
            demoScopeService.assertComplaintAccessible(complaint);
        }
        return mappingService.toComplaintResponse(complaint, fileBaseUrl);
    }

    // ─── Update status (warden only) ─────────────────────────────────────────

    public ComplaintResponse updateStatus(Long id, ComplaintStatusUpdateRequest req) {
        Complaint complaint = findEntityById(id);
        if (demoScopeService != null) {
            demoScopeService.assertComplaintAccessible(complaint);
        }

        ComplaintStatus newStatus = ComplaintStatus.fromDisplayName(req.getStatus());
        if (newStatus == complaint.getStatus() && req.getAdminNotes() == null) {
            throw new BadRequestException("No changes provided.");
        }

        complaint.setStatus(newStatus);

        if (req.getAdminNotes() != null) {
            complaint.setAdminNotes(req.getAdminNotes());
        }

        if (req.getPriority() != null && !req.getPriority().isBlank()) {
            try {
                complaint.setPriority(ComplaintPriority.valueOf(req.getPriority()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (newStatus == ComplaintStatus.Resolved) {
            complaint.setResolvedAt(LocalDateTime.now());
            if (complaint.getStudent() != null && complaint.getStudent().getUser() != null) {
                notificationService.notifyComplaintResolved(
                    complaint.getStudent().getUser().getId(),
                    complaint.getId(),
                    complaint.getComplaintNumber()
                );
            }
        } else {
            if (complaint.getStudent() != null && complaint.getStudent().getUser() != null) {
                notificationService.notifyComplaintStatusChanged(
                    complaint.getStudent().getUser().getId(),
                    complaint.getId(),
                    complaint.getComplaintNumber(),
                    newStatus.getDisplayName()
                );
            }
        }

        // Add timeline entry
        String note = req.getAdminNotes() != null
                ? "Admin: " + req.getAdminNotes()
                : "Status updated to " + newStatus.getDisplayName();
        complaint.getTimeline().add(new ComplaintTimeline(complaint, newStatus, note));

        complaint = complaintRepository.save(complaint);
        return mappingService.toComplaintResponse(complaint, fileBaseUrl);
    }

    // ─── Reopen complaint (student only) ────────────────────────────────────

    public ComplaintResponse reopenComplaint(Long id, Long studentUserId, String reason) {
        Complaint complaint = findEntityById(id);
        if (demoScopeService != null) {
            demoScopeService.assertComplaintAccessible(complaint);
        }

        // Check student ownership
        if (!complaint.getStudent().getUser().getId().equals(studentUserId)) {
            throw new ForbiddenException("You may only reopen your own complaints");
        }

        // Prevent reopening already reopened complaints
        if (complaint.isReopened()) {
            throw new BadRequestException("Complaint is already reopened");
        }

        // Validate complaint is eligible for reopening
        if (complaint.getStatus() != ComplaintStatus.Resolved && complaint.getStatus() != ComplaintStatus.Rejected) {
            throw new BadRequestException("Complaint can only be reopened if it is Resolved or Rejected");
        }

        // Update complaint status and reopening fields
        complaint.setStatus(ComplaintStatus.Under_Review);
        complaint.setReopened(true);
        complaint.setReopenedAt(LocalDateTime.now());
        complaint.setReopenCount(complaint.getReopenCount() + 1);

        // Add timeline entry
        String timelineNote = reason != null && !reason.isBlank()
                ? "Reopened by student: " + reason
                : "Complaint reopened by student";
        complaint.getTimeline().add(new ComplaintTimeline(complaint, ComplaintStatus.Under_Review, timelineNote));

        complaint = complaintRepository.save(complaint);

        // Create audit log entry
        auditService.logComplaintReopening(
                complaint.getId(),
                complaint.getStudent().getUser(),
                reason != null ? reason : "No reason provided"
        );

        // Notify assigned warden (if any)
        if (complaint.getAssignedWarden() != null) {
            notificationService.notifyComplaintReopened(
                    complaint.getAssignedWarden().getUser().getId(),
                    complaint.getId(),
                    complaint.getComplaintNumber()
            );
        }

        return mappingService.toComplaintResponse(complaint, fileBaseUrl);
    }

    // ─── Internal helper ──────────────────────────────────────────────────────

    public Complaint findEntityById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", id));
    }
}
