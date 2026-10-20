package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.ComplaintCreateRequest;
import com.hostelsync.backend.dto.ComplaintResponse;
import com.hostelsync.backend.dto.ComplaintStatusUpdateRequest;
import com.hostelsync.backend.entity.Student;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.ComplaintService;
import com.hostelsync.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * POST   /api/complaints             — student: submit complaint (multipart/form-data)
 * GET    /api/complaints             — warden: all complaints; student always returns own
 * GET    /api/complaints/my          — student: own complaints (uses JWT identity)
 * GET    /api/complaints/{id}        — authenticated: single complaint (IDOR checked in service)
 * PATCH  /api/complaints/{id}/status — warden: update status/notes/priority
 */
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final StudentService studentService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ComplaintController(ComplaintService complaintService,
                               StudentService studentService,
                               UserRepository userRepository,
                               ObjectMapper objectMapper) {
        this.complaintService = complaintService;
        this.studentService = studentService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    // ─── Create complaint (student only) ─────────────────────────────────────

    /**
     * Accepts multipart/form-data with:
     *   - "complaint": JSON blob (ComplaintCreateRequest)
     *   - "files": zero or more file parts
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ComplaintResponse> createComplaint(
            @RequestPart("complaint") String complaintJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails principal) throws Exception {

        ComplaintCreateRequest req;
        try {
            req = objectMapper.readValue(complaintJson, ComplaintCreateRequest.class);
        } catch (Exception e) {
            throw new BadRequestException("Invalid complaint JSON: " + e.getMessage());
        }

        if (req.getSubject() == null || req.getSubject().isBlank()) {
            throw new BadRequestException("Subject is required");
        }
        if (req.getDescription() == null || req.getDescription().length() < 15) {
            throw new BadRequestException("Description must be at least 15 characters");
        }
        if (req.getLocation() == null || req.getLocation().isBlank()) {
            throw new BadRequestException("Location is required");
        }

        User caller = findUser(principal.getUsername());
        Student student = studentService.findEntityByUser(caller);

        ComplaintResponse created = complaintService.createComplaint(req, files, student);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── List complaints ──────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getComplaints(
            @RequestParam(required = false) String studentId,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        if (caller.getRole() == UserRole.WARDEN) {
            return ResponseEntity.ok(complaintService.getAllComplaints());
        }

        // Student — always returns only their own complaints
        Student student = studentService.findEntityByUser(caller);
        return ResponseEntity.ok(complaintService.getComplaintsForStudent(student));
    }

    // ─── Student: my complaints ───────────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());
        Student student = studentService.findEntityByUser(caller);
        return ResponseEntity.ok(complaintService.getComplaintsForStudent(student));
    }

    // ─── Single complaint ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());
        return ResponseEntity.ok(complaintService.getComplaintById(id, caller));
    }

    // ─── Update status (warden only, enforced by SecurityConfig) ─────────────

    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintStatusUpdateRequest request) {

        return ResponseEntity.ok(complaintService.updateStatus(id, request));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
