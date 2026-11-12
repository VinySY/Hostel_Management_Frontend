package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Assignment of a complaint to a warden or other responsible staff.
 * Tracks who is responsible for resolving the complaint.
 */
@Entity
@Table(name = "complaint_assignments",
       indexes = {
           @Index(columnList = "complaint_id"),
           @Index(columnList = "assigned_warden_id"),
           @Index(columnList = "assigned_at")
       })
public class ComplaintAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_warden_id", nullable = false)
    private Warden assignedWarden;

    /** Who made this assignment */
    @NotNull
    @Column(name = "assigned_by_id", nullable = false)
    private Long assignedById;

    /** Reason for assignment/reassignment */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }

    // -------- Constructors --------

    public ComplaintAssignment() {}

    public ComplaintAssignment(Complaint complaint, Warden assignedWarden, Long assignedById, String reason) {
        this.complaint = complaint;
        this.assignedWarden = assignedWarden;
        this.assignedById = assignedById;
        this.reason = reason;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public Warden getAssignedWarden() { return assignedWarden; }
    public void setAssignedWarden(Warden assignedWarden) { this.assignedWarden = assignedWarden; }

    public Long getAssignedById() { return assignedById; }
    public void setAssignedById(Long assignedById) { this.assignedById = assignedById; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
}
