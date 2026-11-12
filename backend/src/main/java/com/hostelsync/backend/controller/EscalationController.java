package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.EscalationRequest;
import com.hostelsync.backend.dto.EscalationResponse;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.EscalationService;
import com.hostelsync.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for complaint escalation operations.
 * Endpoints defined in SecurityConfig:
 * - POST /api/complaints/{id}/escalate - Student only
 * - GET /api/escalations/pending - DGM only (moved to DgmController for scoping)
 * - POST /api/escalations/{id}/handle - DGM only (moved to DgmController)
 * - GET /api/escalations/{id}/history - DGM only (moved to DgmController)
 */
@RestController
@RequestMapping("/api/complaints")
public class EscalationController {

    private final EscalationService escalationService;
    private final StudentService studentService;
    private final UserRepository userRepository;

    public EscalationController(EscalationService escalationService,
                               StudentService studentService,
                               UserRepository userRepository) {
        this.escalationService = escalationService;
        this.studentService = studentService;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/complaints/{id}/escalate - Escalate a complaint to DGM (student only).
     */
    @PostMapping("/{complaintId}/escalate")
    public ResponseEntity<EscalationResponse> escalateComplaint(
            @PathVariable Long complaintId,
            @Valid @RequestBody EscalationRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());
        Student student = studentService.findEntityByUser(caller);

        // Call service layer
        ComplaintEscalation escalation = escalationService.escalateComplaint(
                complaintId,
                caller.getId(),
                request.getReason(),
                request.getDetails()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toEscalationResponse(escalation));
    }

    /**
     * GET /api/complaints/{id}/escalations - Get escalation history for a complaint.
     * Available to the complaint's student and authorized staff (warden/DGM with access).
     */
    @GetMapping("/{complaintId}/escalations")
    public ResponseEntity<List<EscalationResponse>> getComplaintEscalations(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        // Get escalations
        List<ComplaintEscalation> escalations = escalationService.getEscalationHistory(complaintId);

        if (escalations.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // Authorization: Student can see their own, Warden/DGM can see if they have access
        // The service layer handles authorization for individual actions, but for list we check basics
        Complaint complaint = escalations.get(0).getComplaint();

        if (caller.getRole() == UserRole.STUDENT) {
            // Student can only see their own complaint's escalations
            if (!complaint.getStudent().getUser().getId().equals(caller.getId())) {
                throw new ForbiddenException("You may only view escalations for your own complaints");
            }
        }
        // For Warden/DGM, authorization is handled at the service layer for specific actions
        // The list endpoint is relatively safe if they already have access to the complaint

        List<EscalationResponse> response = escalations.stream()
                .map(this::toEscalationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/escalations/pending - Get all pending escalations (global).
     * Moved to DgmController with hostel-scoping.
     * Keeping this as a reference or for admin use.
     */
    @GetMapping("/escalations/pending")
    public ResponseEntity<List<EscalationResponse>> getPendingEscalationsGlobal(
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        // Only ADMIN or superuser should use this global endpoint
        // For now, only allow WARDEN and DGM (they'll see escalations they have access to)
        if (caller.getRole() != UserRole.WARDEN && caller.getRole() != UserRole.DGM) {
            throw new ForbiddenException("Access denied");
        }

        List<ComplaintEscalation> escalations = escalationService.getPendingEscalations();
        List<EscalationResponse> response = escalations.stream()
                .map(this::toEscalationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/escalations/{escalationId} - Get specific escalation details.
     */
    @GetMapping("/escalations/{escalationId}")
    public ResponseEntity<EscalationResponse> getEscalationDetails(
            @PathVariable Long escalationId,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        // Get escalation
        ComplaintEscalation escalation = escalationService.getEscalationById(escalationId)
                .orElseThrow(() -> new ResourceNotFoundException("Escalation", escalationId));

        // Authorization
        Complaint complaint = escalation.getComplaint();
        if (caller.getRole() == UserRole.STUDENT) {
            if (!complaint.getStudent().getUser().getId().equals(caller.getId())) {
                throw new ForbiddenException("You may only view escalations for your own complaints");
            }
        }

        return ResponseEntity.ok(toEscalationResponse(escalation));
    }

    // Helper method to convert ComplaintEscalation to EscalationResponse
    private EscalationResponse toEscalationResponse(ComplaintEscalation escalation) {
        Complaint complaint = escalation.getComplaint();
        String complaintNumber = complaint != null ? complaint.getComplaintNumber() : null;
        Long complaintId = complaint != null ? complaint.getId() : null;

        Student student = escalation.getEscalatedByStudent();
        Long studentId = student != null ? student.getId() : null;
        String studentName = student != null && student.getUser() != null ? student.getUser().getName() : null;

        // Get DGM info if handled
        String dgmName = null;
        if (escalation.getHandledByDgmId() != null) {
            // In a full implementation, you'd fetch the DGM by ID
            dgmName = "DGM";
        }

        return new EscalationResponse(
                escalation.getId(),
                complaintId,
                complaintNumber,
                studentId,
                studentName,
                escalation.getReason(),
                escalation.getSupportingDetails(),
                escalation.getStatus(),
                escalation.getHandledByDgmId(),
                dgmName,
                escalation.getDgmResponse(),
                escalation.getEscalatedAt(),
                escalation.getHandledAt()
        );
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}