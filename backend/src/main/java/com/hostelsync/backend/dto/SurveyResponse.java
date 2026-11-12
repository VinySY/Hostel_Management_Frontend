package com.hostelsync.backend.dto;

import java.time.LocalDateTime;

public class SurveyResponse {

    private Long id;
    private String title;
    private String description;
    private Long targetHostelId;
    private String targetHostelName;
    private boolean active;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String createdBy;
    private LocalDateTime createdAt;
    private long responseCount;
    private boolean hasSubmitted;

    public SurveyResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getTargetHostelId() { return targetHostelId; }
    public void setTargetHostelId(Long targetHostelId) { this.targetHostelId = targetHostelId; }

    public String getTargetHostelName() { return targetHostelName; }
    public void setTargetHostelName(String targetHostelName) { this.targetHostelName = targetHostelName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public long getResponseCount() { return responseCount; }
    public void setResponseCount(long responseCount) { this.responseCount = responseCount; }

    public boolean isHasSubmitted() { return hasSubmitted; }
    public void setHasSubmitted(boolean hasSubmitted) { this.hasSubmitted = hasSubmitted; }
}
