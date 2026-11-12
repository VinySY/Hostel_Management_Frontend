package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.AuditLogResponse;
import com.hostelsync.backend.entity.AuditLog;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.AuditService;
import com.hostelsync.backend.service.DemoScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for audit log queries.
 * Endpoints defined in SecurityConfig: authenticated users only.
 * Pagination is supported via Spring Data Pageable.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditService auditService;
    private final UserRepository userRepository;
    private final com.hostelsync.backend.repository.ComplaintRepository complaintRepository;
    private DemoScopeService demoScopeService;

    public AuditLogController(AuditService auditService,
                              UserRepository userRepository,
                              com.hostelsync.backend.repository.ComplaintRepository complaintRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.complaintRepository = complaintRepository;
    }

    @Autowired(required = false)
    public void setDemoScopeService(DemoScopeService demoScopeService) {
        this.demoScopeService = demoScopeService;
    }

    /**
     * GET /api/audit-logs/entity/{entityType}/{entityId} - Get audit logs for a specific entity.
     * Paginated with page and size parameters.
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsForEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        // Authorization: Students can only view logs for their own user profile or their own complaints
        if (caller.getRole() == com.hostelsync.backend.entity.UserRole.STUDENT) {
            if ("Complaint".equalsIgnoreCase(entityType)) {
                verifyStudentOwnsComplaint(caller, entityId);
            } else if ("User".equalsIgnoreCase(entityType)) {
                if (!caller.getId().equals(entityId)) {
                    throw new ForbiddenException("You may only view your own audit logs");
                }
            } else {
                throw new ForbiddenException("Students may only view audit logs for their own profile or complaints");
            }
        }
        if (demoScopeService != null) {
            demoScopeService.assertAuditEntityAccessible(entityType, entityId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLog> auditLogs = auditService.getAuditLogsForEntity(entityType, entityId, pageable);

        Page<AuditLogResponse> response = auditLogs.map(this::toAuditLogResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/audit-logs/user/{userId} - Get audit logs by user.
     * Users can only see their own audit logs, unless they have admin privileges.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        // Authorization: Users can only see their own audit logs
        if (!caller.getId().equals(userId)) {
            throw new ForbiddenException("You may only view your own audit logs");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLog> auditLogs = auditService.getAuditLogsByUser(userId, pageable);

        Page<AuditLogResponse> response = auditLogs.map(this::toAuditLogResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/audit-logs/complaint/{complaintId} - Convenience endpoint for complaint audit logs.
     */
    @GetMapping("/complaint/{complaintId}")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsForComplaint(
            @PathVariable Long complaintId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        if (caller.getRole() == com.hostelsync.backend.entity.UserRole.STUDENT) {
            verifyStudentOwnsComplaint(caller, complaintId);
        }
        if (demoScopeService != null) {
            demoScopeService.assertComplaintIdAccessible(complaintId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLog> auditLogs = auditService.getAuditLogsForEntity("Complaint", complaintId, pageable);

        Page<AuditLogResponse> response = auditLogs.map(this::toAuditLogResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/audit-logs/search - Search audit logs with optional filters.
     * Supports entityType, entityId, userId, and date range filters.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<AuditLogResponse>> searchAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        if (caller.getRole() == com.hostelsync.backend.entity.UserRole.STUDENT) {
            if (userId != null && !userId.equals(caller.getId())) {
                throw new ForbiddenException("You may only view your own audit logs");
            }
            if (entityType != null && "Complaint".equalsIgnoreCase(entityType) && entityId != null) {
                verifyStudentOwnsComplaint(caller, entityId);
            } else if (entityType != null && "User".equalsIgnoreCase(entityType) && entityId != null) {
                if (!caller.getId().equals(entityId)) {
                    throw new ForbiddenException("You may only view your own audit logs");
                }
            } else if (entityType != null && entityId != null) {
                throw new ForbiddenException("Students may only view audit logs for their own profile or complaints");
            }
        }
        if (demoScopeService != null && demoScopeService.isCurrentCallerDemo()) {
            if (userId != null && !userId.equals(caller.getId())) {
                throw new ForbiddenException("You may only view your own audit logs");
            }
            if (entityType != null && entityId != null) {
                demoScopeService.assertAuditEntityAccessible(entityType, entityId);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        Page<AuditLog> auditLogs;
        if (userId != null && userId.equals(caller.getId())) {
            auditLogs = auditService.getAuditLogsByUser(userId, pageable);
        } else if (entityType != null && entityId != null) {
            auditLogs = auditService.getAuditLogsForEntity(entityType, entityId, pageable);
        } else {
            auditLogs = Page.empty(pageable);
        }

        Page<AuditLogResponse> response = auditLogs.map(this::toAuditLogResponse);
        return ResponseEntity.ok(response);
    }

    private void verifyStudentOwnsComplaint(User studentUser, Long complaintId) {
        com.hostelsync.backend.entity.Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new com.hostelsync.backend.exception.ResourceNotFoundException("Complaint", complaintId));
        if (!complaint.getStudent().getUser().getId().equals(studentUser.getId())) {
            throw new ForbiddenException("You may only view audit logs for your own complaints");
        }
    }

    /**
     * GET /api/audit-logs/my - Get the authenticated user's own audit logs.
     */
    @GetMapping("/my")
    public ResponseEntity<Page<AuditLogResponse>> getMyAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLog> auditLogs = auditService.getAuditLogsByUser(caller.getId(), pageable);

        Page<AuditLogResponse> response = auditLogs.map(this::toAuditLogResponse);
        return ResponseEntity.ok(response);
    }

    // Helper method to convert AuditLog to AuditLogResponse
    private AuditLogResponse toAuditLogResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getAction(),
                auditLog.getUserId(),
                auditLog.getUserName(),
                auditLog.getUserRole(),
                auditLog.getPreviousValue(),
                auditLog.getNewValue(),
                auditLog.getReason(),
                auditLog.getClientInfo(),
                auditLog.getActionTimestamp()
        );
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}