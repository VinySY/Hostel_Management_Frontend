package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Records when a student escalates a complaint to DGM.
 * Captures the escalation reason, timestamp, and DGM response.
 */
@Entity
@Table(name = "complaint_escalations",
       indexes = {
           @Index(columnList = "complaint_id"),
           @Index(columnList = "escalated_at")
       })
public class ComplaintEscalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student escalatedByStudent;

    @NotBlank
    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    /** Optional supporting evidence/details */
    @Column(name = "supporting_details", columnDefinition = "TEXT")
    private String supportingDetails;

    @Column(name = "escalated_at", nullable = false, updatable = false)
    private LocalDateTime escalatedAt;

    /** DGM's response or action taken */
    @Column(name = "dgm_response", columnDefinition = "TEXT")
    private String dgmResponse;

    /** DGM who took action */
    @Column(name = "handled_by_dgm_id")
    private Long handledByDgmId;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    /** Status of escalation: PENDING, IN_REVIEW, RESOLVED, REJECTED */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EscalationStatus status = EscalationStatus.PENDING;

    @PrePersist
    protected void onCreate() {
        escalatedAt = LocalDateTime.now();
    }

    // -------- Constructors --------

    public ComplaintEscalation() {}

    public ComplaintEscalation(Complaint complaint, Student escalatedByStudent, String reason) {
        this.complaint = complaint;
        this.escalatedByStudent = escalatedByStudent;
        this.reason = reason;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public Student getEscalatedByStudent() { return escalatedByStudent; }
    public void setEscalatedByStudent(Student escalatedByStudent) { this.escalatedByStudent = escalatedByStudent; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSupportingDetails() { return supportingDetails; }
    public void setSupportingDetails(String supportingDetails) { this.supportingDetails = supportingDetails; }

    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }

    public String getDgmResponse() { return dgmResponse; }
    public void setDgmResponse(String dgmResponse) { this.dgmResponse = dgmResponse; }

    public Long getHandledByDgmId() { return handledByDgmId; }
    public void setHandledByDgmId(Long handledByDgmId) { this.handledByDgmId = handledByDgmId; }

    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }

    public EscalationStatus getStatus() { return status; }
    public void setStatus(EscalationStatus status) { this.status = status; }
}
