package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.FeedbackTaskCreateRequest;
import com.hostelsync.backend.dto.FeedbackTaskResponse;
import com.hostelsync.backend.dto.FeedbackTaskUpdateRequest;
import com.hostelsync.backend.service.FeedbackTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback-tasks")
public class FeedbackTaskController {

    private final FeedbackTaskService feedbackTaskService;

    public FeedbackTaskController(FeedbackTaskService feedbackTaskService) {
        this.feedbackTaskService = feedbackTaskService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<List<FeedbackTaskResponse>> getTasks(Authentication authentication,
                                                               @RequestParam(value = "hostelId", required = false) Long hostelId) {
        boolean isDgm = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_DGM"));

        if (isDgm) {
            return ResponseEntity.ok(feedbackTaskService.getAllTasks(hostelId));
        } else {
            String email = authentication != null ? authentication.getName() : "";
            return ResponseEntity.ok(feedbackTaskService.getTasksForWarden(email));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('DGM')")
    public ResponseEntity<FeedbackTaskResponse> createTask(@Valid @RequestBody FeedbackTaskCreateRequest request,
                                                           Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "DGM";
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackTaskService.createTask(request, email));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<FeedbackTaskResponse> updateTaskStatus(@PathVariable Long id,
                                                                 @Valid @RequestBody FeedbackTaskUpdateRequest request,
                                                                 Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "STAFF";
        return ResponseEntity.ok(feedbackTaskService.updateTaskStatus(id, request, email));
    }
}
