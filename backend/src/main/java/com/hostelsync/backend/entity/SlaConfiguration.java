package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * SLA (Service Level Agreement) configuration for complaint resolution deadlines.
 * Defines how long each priority level should take to resolve.
 */
@Entity
@Table(name = "sla_configurations")
public class SlaConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private ComplaintPriority priority;

    /** Resolution deadline in hours */
    @Min(0)
    @Column(name = "deadline_hours", nullable = false)
    private int deadlineHours;

    /** First reminder at this percentage of deadline (e.g., 50 for 50%) */
    @Min(0)
    @Column(name = "reminder_threshold_percent", nullable = false)
    private int reminderThresholdPercent = 50;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // -------- Constructors --------

    public SlaConfiguration() {}

    public SlaConfiguration(ComplaintPriority priority, int deadlineHours) {
        this.priority = priority;
        this.deadlineHours = deadlineHours;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ComplaintPriority getPriority() { return priority; }
    public void setPriority(ComplaintPriority priority) { this.priority = priority; }

    public int getDeadlineHours() { return deadlineHours; }
    public void setDeadlineHours(int deadlineHours) { this.deadlineHours = deadlineHours; }

    public int getReminderThresholdPercent() { return reminderThresholdPercent; }
    public void setReminderThresholdPercent(int reminderThresholdPercent) { this.reminderThresholdPercent = reminderThresholdPercent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
