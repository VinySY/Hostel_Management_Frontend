package com.hostelsync.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FeedbackTaskCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 3000)
    private String description;

    private Long surveyId;

    @NotNull(message = "Hostel ID is required")
    private Long hostelId;

    @NotNull(message = "Warden ID is required")
    private Long wardenId;

    private String category = "GENERAL";
    private String priority = "MEDIUM";

    public FeedbackTaskCreateRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }

    public Long getHostelId() { return hostelId; }
    public void setHostelId(Long hostelId) { this.hostelId = hostelId; }

    public Long getWardenId() { return wardenId; }
    public void setWardenId(Long wardenId) { this.wardenId = wardenId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
