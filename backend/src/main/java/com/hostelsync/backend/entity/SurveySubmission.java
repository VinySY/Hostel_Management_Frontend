package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "survey_submissions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"survey_id", "student_id"}))
public class SurveySubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // 10 structured rating questions (1-5)
    @Min(1) @Max(5)
    @Column(name = "q1_cleanliness", nullable = false)
    private int q1Cleanliness;

    @Min(1) @Max(5)
    @Column(name = "q2_washrooms", nullable = false)
    private int q2Washrooms;

    @Min(1) @Max(5)
    @Column(name = "q3_furniture", nullable = false)
    private int q3Furniture;

    @Min(1) @Max(5)
    @Column(name = "q4_drinking_water", nullable = false)
    private int q4DrinkingWater;

    @Min(1) @Max(5)
    @Column(name = "q5_wifi", nullable = false)
    private int q5Wifi;

    @Min(1) @Max(5)
    @Column(name = "q6_electricity", nullable = false)
    private int q6Electricity;

    @Min(1) @Max(5)
    @Column(name = "q7_food", nullable = false)
    private int q7Food;

    @Min(1) @Max(5)
    @Column(name = "q8_security", nullable = false)
    private int q8Security;

    @Min(1) @Max(5)
    @Column(name = "q9_warden_support", nullable = false)
    private int q9WardenSupport;

    @Min(1) @Max(5)
    @Column(name = "q10_complaint_resolution", nullable = false)
    private int q10ComplaintResolution;

    // Optional written comments (max 30 words enforced in service & DTO)
    @Size(max = 500)
    @Column(length = 500)
    private String comments;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }

    public SurveySubmission() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Survey getSurvey() { return survey; }
    public void setSurvey(Survey survey) { this.survey = survey; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

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

    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
