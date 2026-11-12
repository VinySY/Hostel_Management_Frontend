package com.hostelsync.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class SurveyAnalyticsResponse {

    private Long surveyId;
    private String surveyTitle;
    private long totalResponses;
    private double overallSatisfactionScore; // e.g. 84.5%

    // Average rating for each of the 10 questions (out of 5.0)
    private double q1CleanlinessAvg;
    private double q2WashroomsAvg;
    private double q3FurnitureAvg;
    private double q4DrinkingWaterAvg;
    private double q5WifiAvg;
    private double q6ElectricityAvg;
    private double q7FoodAvg;
    private double q8SecurityAvg;
    private double q9WardenSupportAvg;
    private double q10ComplaintResolutionAvg;

    private List<SurveyCommentDto> comments = new ArrayList<>();

    public SurveyAnalyticsResponse() {}

    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }

    public String getSurveyTitle() { return surveyTitle; }
    public void setSurveyTitle(String surveyTitle) { this.surveyTitle = surveyTitle; }

    public long getTotalResponses() { return totalResponses; }
    public void setTotalResponses(long totalResponses) { this.totalResponses = totalResponses; }

    public double getOverallSatisfactionScore() { return overallSatisfactionScore; }
    public void setOverallSatisfactionScore(double overallSatisfactionScore) { this.overallSatisfactionScore = overallSatisfactionScore; }

    public double getQ1CleanlinessAvg() { return q1CleanlinessAvg; }
    public void setQ1CleanlinessAvg(double q1CleanlinessAvg) { this.q1CleanlinessAvg = q1CleanlinessAvg; }

    public double getQ2WashroomsAvg() { return q2WashroomsAvg; }
    public void setQ2WashroomsAvg(double q2WashroomsAvg) { this.q2WashroomsAvg = q2WashroomsAvg; }

    public double getQ3FurnitureAvg() { return q3FurnitureAvg; }
    public void setQ3FurnitureAvg(double q3FurnitureAvg) { this.q3FurnitureAvg = q3FurnitureAvg; }

    public double getQ4DrinkingWaterAvg() { return q4DrinkingWaterAvg; }
    public void setQ4DrinkingWaterAvg(double q4DrinkingWaterAvg) { this.q4DrinkingWaterAvg = q4DrinkingWaterAvg; }

    public double getQ5WifiAvg() { return q5WifiAvg; }
    public void setQ5WifiAvg(double q5WifiAvg) { this.q5WifiAvg = q5WifiAvg; }

    public double getQ6ElectricityAvg() { return q6ElectricityAvg; }
    public void setQ6ElectricityAvg(double q6ElectricityAvg) { this.q6ElectricityAvg = q6ElectricityAvg; }

    public double getQ7FoodAvg() { return q7FoodAvg; }
    public void setQ7FoodAvg(double q7FoodAvg) { this.q7FoodAvg = q7FoodAvg; }

    public double getQ8SecurityAvg() { return q8SecurityAvg; }
    public void setQ8SecurityAvg(double q8SecurityAvg) { this.q8SecurityAvg = q8SecurityAvg; }

    public double getQ9WardenSupportAvg() { return q9WardenSupportAvg; }
    public void setQ9WardenSupportAvg(double q9WardenSupportAvg) { this.q9WardenSupportAvg = q9WardenSupportAvg; }

    public double getQ10ComplaintResolutionAvg() { return q10ComplaintResolutionAvg; }
    public void setQ10ComplaintResolutionAvg(double q10ComplaintResolutionAvg) { this.q10ComplaintResolutionAvg = q10ComplaintResolutionAvg; }

    public List<SurveyCommentDto> getComments() { return comments; }
    public void setComments(List<SurveyCommentDto> comments) { this.comments = comments; }
}
