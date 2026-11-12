package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.SurveyAnalyticsResponse;
import com.hostelsync.backend.dto.SurveyCreateRequest;
import com.hostelsync.backend.dto.SurveyResponse;
import com.hostelsync.backend.dto.SurveySubmissionRequest;
import com.hostelsync.backend.service.SurveyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<List<SurveyResponse>> getAllSurveys() {
        return ResponseEntity.ok(surveyService.getAllSurveys());
    }

    @GetMapping("/active")
    public ResponseEntity<List<SurveyResponse>> getActiveSurveys(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "";
        return ResponseEntity.ok(surveyService.getActiveSurveysForStudent(email));
    }

    @PostMapping
    @PreAuthorize("hasRole('DGM')")
    public ResponseEntity<SurveyResponse> createSurvey(@Valid @RequestBody SurveyCreateRequest request,
                                                       Authentication authentication) {
        String creator = authentication != null ? authentication.getName() : "DGM";
        return ResponseEntity.status(HttpStatus.CREATED).body(surveyService.createSurvey(request, creator));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, String>> submitSurvey(@PathVariable Long id,
                                                           @Valid @RequestBody SurveySubmissionRequest request,
                                                           Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "";
        surveyService.submitSurvey(id, request, email);
        return ResponseEntity.ok(Map.of("message", "Survey submitted successfully"));
    }

    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<SurveyAnalyticsResponse> getSurveyAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(surveyService.getSurveyAnalytics(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('DGM')")
    public ResponseEntity<SurveyResponse> toggleSurveyActive(@PathVariable Long id,
                                                             @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(surveyService.toggleSurveyActive(id, active));
    }
}
