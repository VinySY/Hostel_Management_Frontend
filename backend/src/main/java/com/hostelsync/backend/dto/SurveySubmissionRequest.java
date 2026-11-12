package com.hostelsync.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class SurveySubmissionRequest {

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int q1Cleanliness = 5;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int q2Washrooms = 5;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int q3Furniture = 5;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int q4DrinkingWater = 5;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int q5Wifi = 5;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int q6Electricity = 5;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int q7Food = 5;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int q8Security = 5;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int q9WardenSupport = 5;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int q10ComplaintResolution = 5;

    @Size(max = 500, message = "Comments must not exceed 500 characters")
    private String comments;

    public SurveySubmissionRequest() {}

    public int getQ1Cleanliness() { return q1Cleanliness; }
    public void setQ1Cleanliness(int q1Cleanliness) { this.q1Cleanliness = q1Cleanliness; }

    public int getQ2Washrooms() { return q2Washrooms; }
    public void setQ2Washrooms(int q2Washrooms) { this.q2Washrooms = q2Washrooms; }

    public int getQ3Furniture() { return q3Furniture; }
    public void setQ3Furniture(int q3Furniture) { this.q3Furniture = q3Furniture; }

    public int getQ4DrinkingWater() { return q4DrinkingWater; }
    public void setQ4DrinkingWater(int q4DrinkingWater) { this.q4DrinkingWater = q4DrinkingWater; }

    public int getQ5Wifi() { return q5Wifi; }
    public void setQ5Wifi(int q5Wifi) { this.q5Wifi = q5Wifi; }

    public int getQ6Electricity() { return q6Electricity; }
    public void setQ6Electricity(int q6Electricity) { this.q6Electricity = q6Electricity; }

    public int getQ7Food() { return q7Food; }
    public void setQ7Food(int q7Food) { this.q7Food = q7Food; }

    public int getQ8Security() { return q8Security; }
    public void setQ8Security(int q8Security) { this.q8Security = q8Security; }

    public int getQ9WardenSupport() { return q9WardenSupport; }
    public void setQ9WardenSupport(int q9WardenSupport) { this.q9WardenSupport = q9WardenSupport; }

    public int getQ10ComplaintResolution() { return q10ComplaintResolution; }
    public void setQ10ComplaintResolution(int q10ComplaintResolution) { this.q10ComplaintResolution = q10ComplaintResolution; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
