package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.DgmDashboardResponse;
import com.hostelsync.backend.dto.DgmResponse;
import com.hostelsync.backend.dto.EscalationRequest;
import com.hostelsync.backend.dto.EscalationResponse;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.repository.DgmRepository;
import com.hostelsync.backend.repository.HostelRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.DemoScopeService;
import com.hostelsync.backend.service.DgmService;
import com.hostelsync.backend.service.EscalationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for DGM operations.
 * Endpoints defined in SecurityConfig: hasRole("DGM") required.
 */
@RestController
@RequestMapping("/api/dgm")
public class DgmController {

    private final DgmService dgmService;
    private final EscalationService escalationService;
    private final HostelRepository hostelRepository;
    private final UserRepository userRepository;
    private final DgmRepository dgmRepository;
    private DemoScopeService demoScopeService;

    public DgmController(DgmService dgmService, EscalationService escalationService,
                        HostelRepository hostelRepository, UserRepository userRepository,
                        DgmRepository dgmRepository) {
        this.dgmService = dgmService;
        this.escalationService = escalationService;
        this.hostelRepository = hostelRepository;
        this.userRepository = userRepository;
        this.dgmRepository = dgmRepository;
    }

    @Autowired(required = false)
    public void setDemoScopeService(DemoScopeService demoScopeService) {
        this.demoScopeService = demoScopeService;
    }

    /**
     * GET /api/dgm/dashboard - Get dashboard statistics for the authenticated DGM.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DgmDashboardResponse> getDashboard(@AuthenticationPrincipal UserDetails principal) {
        User caller = findUser(principal.getUsername());
        DgmDashboardResponse response = dgmService.getDashboardResponse(caller.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/dgm/hostels - Get hostels assigned to the authenticated DGM.
     */
    @GetMapping("/hostels")
    public ResponseEntity<List<Hostel>> getAssignedHostels(@AuthenticationPrincipal UserDetails principal) {
        User caller = findUser(principal.getUsername());
        Dgm dgm = dgmService.getDgmByUserId(caller.getId());
        List<Hostel> hostels = dgmService.getHostelsForDgm(dgm.getId());
        return ResponseEntity.ok(hostels);
    }

    /**
     * GET /api/escalations/pending - Get pending escalations for the authenticated DGM's hostels.
     */
    @GetMapping("/escalations/pending")
    public ResponseEntity<List<EscalationResponse>> getPendingEscalations(@AuthenticationPrincipal UserDetails principal) {
        User caller = findUser(principal.getUsername());
        Dgm dgm = dgmService.getDgmByUserId(caller.getId());

        // Get hostels assigned to DGM
        List<Hostel> hostels = dgmService.getHostelsForDgm(dgm.getId());
        List<Long> hostelIds = hostels.stream().map(Hostel::getId).collect(Collectors.toList());

        // Get all pending escalations
        List<ComplaintEscalation> pendingEscalations = escalationService.getPendingEscalations();

        // Filter to only escalations from DGM's assigned hostels
        List<EscalationResponse> filtered = pendingEscalations.stream()
                .filter(escalation -> {
                    Complaint complaint = escalation.getComplaint();
                    if (complaint.getStudent().getRoom() == null ||
                        complaint.getStudent().getRoom().getHostel() == null) {
                        return false;
                    }
                    Long hostelId = complaint.getStudent().getRoom().getHostel().getId();
                    return hostelIds.contains(hostelId);
                })
                .map(this::toEscalationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(filtered);
    }

    /**
     * POST /api/dgm/complaints/{complaintId}/escalate - Initiate DGM executive escalation on a complaint.
     */
    @PostMapping("/complaints/{complaintId}/escalate")
    public ResponseEntity<EscalationResponse> escalateComplaintAsDgm(
            @PathVariable Long complaintId,
            @Valid @RequestBody EscalationRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User caller = findUser(principal.getUsername());
        ComplaintEscalation escalation = escalationService.escalateComplaintByDgm(
                complaintId,
                caller.getId(),
                request.getReason(),
                request.getDetails()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toEscalationResponse(escalation));
    }

    /**
     * POST /api/escalations/{id}/handle - Handle an escalation.
     * Requires:
     * - DGM has access to the hostel
     * - Escalation is pending or in review
     */
    @PostMapping("/escalations/{id}/handle")
    public ResponseEntity<EscalationResponse> handleEscalation(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails principal) {

        String dgmResponse = request.get("response");
        String statusStr = request.get("status");

        if (dgmResponse == null || dgmResponse.isBlank()) {
            throw new IllegalArgumentException("Response is required");
        }

        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        EscalationStatus status;
        if ("DISMISSED".equalsIgnoreCase(statusStr)) {
            status = EscalationStatus.REJECTED;
        } else {
            try {
                status = EscalationStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + statusStr + ". Must be one of: PENDING, IN_REVIEW, RESOLVED, REJECTED");
            }
        }

        User caller = findUser(principal.getUsername());
        ComplaintEscalation handled = escalationService.handleEscalation(id, caller.getId(), dgmResponse, status);

        return ResponseEntity.ok(toEscalationResponse(handled));
    }

    /**
     * GET /api/escalations/{id}/history - Get escalation history for a complaint.
     */
    @GetMapping("/escalations/{id}/history")
    public ResponseEntity<List<EscalationResponse>> getEscalationHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());
        Dgm dgm = dgmService.getDgmByUserId(caller.getId());
        if (demoScopeService != null) {
            demoScopeService.assertComplaintIdAccessible(id);
        }

        // Verify DGM has access to this complaint's hostel
        List<ComplaintEscalation> escalations = escalationService.getEscalationHistory(id);
        if (!escalations.isEmpty()) {
            Complaint complaint = escalations.get(0).getComplaint();
            if (complaint.getStudent().getRoom() != null &&
                complaint.getStudent().getRoom().getHostel() != null) {

                Long hostelId = complaint.getStudent().getRoom().getHostel().getId();
                if (!dgmService.hasAccessToHostel(dgm.getId(), hostelId)) {
                    throw new ForbiddenException("You are not authorized to view escalations for this hostel");
                }
            }
        }

        List<EscalationResponse> response = escalations.stream()
                .map(this::toEscalationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/dgm/profile - Get DGM profile info.
     */
    @GetMapping("/profile")
    public ResponseEntity<DgmResponse> getProfile(@AuthenticationPrincipal UserDetails principal) {
        User caller = findUser(principal.getUsername());
        Dgm dgm = dgmService.getDgmByUserId(caller.getId());

        DgmResponse response = DgmResponse.fromUserAndDgmId(
                caller,
                dgm.getId(),
                dgm.getEmployeeId()
        );

        return ResponseEntity.ok(response);
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
            dgmName = dgmRepository.findById(escalation.getHandledByDgmId())
                    .map(d -> d.getUser() != null ? d.getUser().getName() : "DGM")
                    .orElse("DGM");
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