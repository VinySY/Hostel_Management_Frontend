package com.hostelsync.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class FeedbackTaskUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status; // "PENDING", "IN_PROGRESS", "RESOLVED"

    private String resolutionNotes;

    public FeedbackTaskUpdateRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
