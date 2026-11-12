package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A complaint submitted by a student.
 * Timeline events and attachments are stored in separate tables.
 */
@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable complaint number, e.g. "CMP-1024".
     * Set by the service layer after the first persist (to use the generated ID).
     * Not blank in normal operation, but nullable=false is enforced by DB after first save.
     */
    @Column(name = "complaint_number", unique = true)
    private String complaintNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintType type;

    @NotBlank
    @Size(max = 300)
    @Column(nullable = false)
    private String subject;

    @NotBlank
    @Size(min = 15, max = 3000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Size(max = 300)
    @Column(nullable = false)
    private String location;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintPriority priority = ComplaintPriority.Medium;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status = ComplaintStatus.Submitted;

    /** Notes added by the warden when managing this complaint */
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** SLA deadline for resolution */
    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    /** When the complaint was marked overdue (null if not overdue) */
    @Column(name = "overdue_at")
    private LocalDateTime overdueAt;

    /** When 50% reminder was sent (null if not sent) */
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    /** Warden currently assigned to this complaint */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_warden_id")
    private Warden assignedWarden;

    /** Whether complaint has been escalated to DGM */
    @Column(name = "escalated", nullable = false)
    private boolean escalated = false;

    /** When complaint was escalated (null if not escalated) */
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    /** Whether complaint was reopened after resolution */
    @Column(name = "reopened", nullable = false)
    private boolean reopened = false;

    /** When complaint was reopened (null if not reopened) */
    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    /** Number of times complaint has been reopened */
    @Column(name = "reopen_count", nullable = false)
    private int reopenCount = 0;

    /** Detailed resolution notes (for resolved complaints) */
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    /** Ordered timeline of status changes */
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    private List<ComplaintTimeline> timeline = new ArrayList<>();

    /** Uploaded evidence files */
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplaintAttachment> attachments = new ArrayList<>();

    /** Assignment history */
    @OneToMany(mappedBy = "complaint", fetch = FetchType.LAZY)
    @OrderBy("assignedAt DESC")
    private List<ComplaintAssignment> assignments = new ArrayList<>();

    /** Escalation history */
    @OneToMany(mappedBy = "complaint", fetch = FetchType.LAZY)
    @OrderBy("escalatedAt DESC")
    private List<ComplaintEscalation> escalations = new ArrayList<>();

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

    public Complaint() {}

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getComplaintNumber() { return complaintNumber; }
    public void setComplaintNumber(String complaintNumber) { this.complaintNumber = complaintNumber; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public ComplaintType getType() { return type; }
    public void setType(ComplaintType type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public ComplaintPriority getPriority() { return priority; }
    public void setPriority(ComplaintPriority priority) { this.priority = priority; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(LocalDateTime slaDeadline) { this.slaDeadline = slaDeadline; }

    public LocalDateTime getOverdueAt() { return overdueAt; }
    public void setOverdueAt(LocalDateTime overdueAt) { this.overdueAt = overdueAt; }

    public LocalDateTime getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(LocalDateTime reminderSentAt) { this.reminderSentAt = reminderSentAt; }

    public Warden getAssignedWarden() { return assignedWarden; }
    public void setAssignedWarden(Warden assignedWarden) { this.assignedWarden = assignedWarden; }

    public boolean isEscalated() { return escalated; }
    public void setEscalated(boolean escalated) { this.escalated = escalated; }

    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }

    public boolean isReopened() { return reopened; }
    public void setReopened(boolean reopened) { this.reopened = reopened; }

    public LocalDateTime getReopenedAt() { return reopenedAt; }
    public void setReopenedAt(LocalDateTime reopenedAt) { this.reopenedAt = reopenedAt; }

    public int getReopenCount() { return reopenCount; }
    public void setReopenCount(int reopenCount) { this.reopenCount = reopenCount; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public List<ComplaintTimeline> getTimeline() { return timeline; }
    public void setTimeline(List<ComplaintTimeline> timeline) { this.timeline = timeline; }

    public List<ComplaintAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<ComplaintAttachment> attachments) { this.attachments = attachments; }

    public List<ComplaintAssignment> getAssignments() { return assignments; }
    public void setAssignments(List<ComplaintAssignment> assignments) { this.assignments = assignments; }

    public List<ComplaintEscalation> getEscalations() { return escalations; }
    public void setEscalations(List<ComplaintEscalation> escalations) { this.escalations = escalations; }
}
