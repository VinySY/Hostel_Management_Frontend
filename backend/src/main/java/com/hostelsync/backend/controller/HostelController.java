package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.HostelCreateRequest;
import com.hostelsync.backend.dto.HostelDetailResponse;
import com.hostelsync.backend.dto.HostelResponse;
import com.hostelsync.backend.dto.HostelUpdateRequest;
import com.hostelsync.backend.service.HostelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hostels")
public class HostelController {

    private final HostelService hostelService;

    public HostelController(HostelService hostelService) {
        this.hostelService = hostelService;
    }

    @GetMapping
    public ResponseEntity<List<HostelResponse>> getAllHostels(
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(hostelService.getAllHostels(includeInactive));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HostelResponse> getHostelById(@PathVariable Long id) {
        return ResponseEntity.ok(hostelService.getHostelById(id));
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<HostelDetailResponse> getHostelDetails(@PathVariable Long id) {
        return ResponseEntity.ok(hostelService.getHostelDetails(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('DGM')")
    public ResponseEntity<HostelResponse> createHostel(@Valid @RequestBody HostelCreateRequest request,
                                                       @AuthenticationPrincipal UserDetails principal) {
        String creatorEmail = principal != null ? principal.getUsername() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hostelService.createHostel(request, creatorEmail));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DGM')")
    public ResponseEntity<HostelResponse> updateHostel(@PathVariable Long id,
                                                       @Valid @RequestBody HostelUpdateRequest request) {
        return ResponseEntity.ok(hostelService.updateHostel(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('DGM')")
    public ResponseEntity<HostelResponse> toggleHostelStatus(@PathVariable Long id,
                                                             @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(hostelService.toggleHostelActive(id, active));
    }
}
