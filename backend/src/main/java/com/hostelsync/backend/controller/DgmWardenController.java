package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.WardenCreateRequest;
import com.hostelsync.backend.dto.WardenFloorAssignRequest;
import com.hostelsync.backend.dto.WardenResponse;
import com.hostelsync.backend.dto.WardenUpdateRequest;
import com.hostelsync.backend.service.DgmWardenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dgm/wardens")
@PreAuthorize("hasRole('DGM')")
public class DgmWardenController {

    private final DgmWardenService dgmWardenService;

    public DgmWardenController(DgmWardenService dgmWardenService) {
        this.dgmWardenService = dgmWardenService;
    }

    @GetMapping
    public ResponseEntity<List<WardenResponse>> getAllWardens(
            @RequestParam(required = false) Long hostelId) {
        return ResponseEntity.ok(dgmWardenService.getAllWardens(hostelId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WardenResponse> getWardenById(@PathVariable Long id) {
        return ResponseEntity.ok(dgmWardenService.getWardenById(id));
    }

    @PostMapping
    public ResponseEntity<WardenResponse> createWarden(@Valid @RequestBody WardenCreateRequest request,
                                                       Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "ANONYMOUS";
        return ResponseEntity.status(HttpStatus.CREATED).body(dgmWardenService.createWarden(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WardenResponse> updateWarden(@PathVariable Long id,
                                                       @Valid @RequestBody WardenUpdateRequest request,
                                                       Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "ANONYMOUS";
        return ResponseEntity.ok(dgmWardenService.updateWarden(id, request, actor));
    }

    @PutMapping("/{id}/assignments")
    public ResponseEntity<WardenResponse> assignFloors(@PathVariable Long id,
                                                       @Valid @RequestBody WardenFloorAssignRequest request,
                                                       Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "ANONYMOUS";
        return ResponseEntity.ok(dgmWardenService.assignFloors(id, request, actor));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WardenResponse> toggleWardenStatus(@PathVariable Long id,
                                                             @RequestBody Map<String, Boolean> body,
                                                             Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "ANONYMOUS";
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(dgmWardenService.toggleWardenStatus(id, active, actor));
    }
}
