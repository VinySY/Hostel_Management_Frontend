package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.SlaConfigurationRequest;
import com.hostelsync.backend.dto.SlaConfigurationResponse;
import com.hostelsync.backend.entity.ComplaintPriority;
import com.hostelsync.backend.entity.SlaConfiguration;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.SlaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for SLA configuration management.
 * Administrative operations (create/update) are restricted to WARDEN and DGM roles.
 */
@RestController
@RequestMapping("/api/sla-configurations")
public class SlaConfigurationController {

    private final SlaService slaService;
    private final UserRepository userRepository;

    public SlaConfigurationController(SlaService slaService, UserRepository userRepository) {
        this.slaService = slaService;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/sla-configurations - List all SLA configurations
     * Returns configurations or defaults for all priorities.
     */
    @GetMapping
    public ResponseEntity<List<SlaConfigurationResponse>> getAllConfigurations() {
        List<SlaConfigurationResponse> configurations = Arrays.stream(ComplaintPriority.values())
                .map(priority -> {
                    SlaConfiguration config = slaService.getConfigurationForPriority(priority);
                    return toResponse(config);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(configurations);
    }

    /**
     * POST /api/sla-configurations - Create SLA configuration for a priority
     */
    @PostMapping
    public ResponseEntity<SlaConfigurationResponse> createConfiguration(
            @Valid @RequestBody SlaConfigurationRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        checkAdminAuthorization(principal);

        SlaConfiguration created = slaService.createConfiguration(
                request.getPriority(),
                request.getDeadlineHours(),
                request.getReminderThresholdPercent()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    /**
     * GET /api/sla-configurations/{idOrPriority} - Get configuration by numeric ID or priority name
     */
    @GetMapping("/{idOrPriority}")
    public ResponseEntity<SlaConfigurationResponse> getConfiguration(
            @PathVariable String idOrPriority) {

        if (isNumeric(idOrPriority)) {
            Long id = Long.parseLong(idOrPriority);
            SlaConfiguration config = slaService.getById(id);
            return ResponseEntity.ok(toResponse(config));
        }

        ComplaintPriority priorityEnum = parsePriority(idOrPriority);
        SlaConfiguration config = slaService.getConfigurationForPriority(priorityEnum);
        return ResponseEntity.ok(toResponse(config));
    }

    /**
     * PUT /api/sla-configurations/{idOrPriority} - Update or create SLA configuration by numeric ID or priority
     */
    @PutMapping("/{idOrPriority}")
    public ResponseEntity<SlaConfigurationResponse> updateConfiguration(
            @PathVariable String idOrPriority,
            @Valid @RequestBody SlaConfigurationRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        checkAdminAuthorization(principal);

        SlaConfiguration updated;
        if (isNumeric(idOrPriority)) {
            Long id = Long.parseLong(idOrPriority);
            updated = slaService.updateConfiguration(
                    id,
                    request.getPriority(),
                    request.getDeadlineHours(),
                    request.getReminderThresholdPercent()
            );
        } else {
            ComplaintPriority priorityEnum = parsePriority(idOrPriority);
            updated = slaService.upsertConfiguration(
                    priorityEnum,
                    request.getDeadlineHours(),
                    request.getReminderThresholdPercent()
            );
        }

        return ResponseEntity.ok(toResponse(updated));
    }

    /**
     * GET /api/sla-configurations/defaults/all - Get default SLA configurations
     */
    @GetMapping("/defaults/all")
    public ResponseEntity<List<SlaConfigurationResponse>> getDefaultConfigurations() {
        List<SlaConfigurationResponse> defaults = Arrays.stream(ComplaintPriority.values())
                .map(priority -> new SlaConfigurationResponse(
                        null,
                        priority,
                        getDefaultDeadlineHours(priority),
                        50
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(defaults);
    }

    private void checkAdminAuthorization(UserDetails principal) {
        if (principal == null) {
            throw new ForbiddenException("Authentication required");
        }
        User caller = findUser(principal.getUsername());
        if (caller.getRole() != UserRole.WARDEN && caller.getRole() != UserRole.DGM) {
            throw new ForbiddenException("Only administrators can modify SLA configurations");
        }
    }

    private ComplaintPriority parsePriority(String text) {
        if (text == null || text.isBlank()) {
            throw new BadRequestException("Priority parameter is required");
        }
        for (ComplaintPriority p : ComplaintPriority.values()) {
            if (p.name().equalsIgnoreCase(text.trim())) {
                return p;
            }
        }
        throw new BadRequestException("Invalid priority: " + text + ". Must be one of: Urgent, High, Medium, Low");
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isBlank()) return false;
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private SlaConfigurationResponse toResponse(SlaConfiguration config) {
        if (config == null) {
            return null;
        }
        return new SlaConfigurationResponse(
                config.getId(),
                config.getPriority(),
                config.getDeadlineHours(),
                config.getReminderThresholdPercent()
        );
    }

    private int getDefaultDeadlineHours(ComplaintPriority priority) {
        return switch (priority) {
            case Urgent -> 2;
            case High -> 8;
            case Medium -> 24;
            case Low -> 48;
        };
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
