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

    /** Base URL for file download links, e.g. http://localhost:8080/api/files */
    private final String fileBaseUrl;

    public ComplaintService(ComplaintRepository complaintRepository,
                            ComplaintAttachmentRepository attachmentRepository,
                            FileStorageService fileStorageService,
                            MappingService mappingService,
                            @Value("${app.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*,http://10.*:*,http://192.168.*:*,http://172.*:*,http://localhost:3000,http://localhost:3001,http://127.0.0.1:3000,http://127.0.0.1:3001,http://10.200.50.197:3000,http://10.200.50.197:3001,http://10.63.156.120:3000,http://10.63.156.120:3001}") String origins,
                            @Value("${server.port:8080}") int port) {
        this.complaintRepository = complaintRepository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.mappingService = mappingService;
        this.fileBaseUrl = "http://localhost:" + port + "/api/files";
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
        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(c -> mappingService.toComplaintResponse(c, fileBaseUrl))
                .toList();
    }

    // ─── Get for a specific student ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaintsForStudent(Student student) {
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
        return mappingService.toComplaintResponse(complaint, fileBaseUrl);
    }

    // ─── Update status (warden only) ─────────────────────────────────────────

    public ComplaintResponse updateStatus(Long id, ComplaintStatusUpdateRequest req) {
        Complaint complaint = findEntityById(id);

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
        }

        // Add timeline entry
        String note = req.getAdminNotes() != null
                ? "Admin: " + req.getAdminNotes()
                : "Status updated to " + newStatus.getDisplayName();
        complaint.getTimeline().add(new ComplaintTimeline(complaint, newStatus, note));

        complaint = complaintRepository.save(complaint);
        return mappingService.toComplaintResponse(complaint, fileBaseUrl);
    }

    // ─── Internal helper ──────────────────────────────────────────────────────

    public Complaint findEntityById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", id));
    }
}
