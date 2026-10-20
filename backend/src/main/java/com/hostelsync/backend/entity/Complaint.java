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

    /** Ordered timeline of status changes */
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    private List<ComplaintTimeline> timeline = new ArrayList<>();

    /** Uploaded evidence files */
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplaintAttachment> attachments = new ArrayList<>();

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

    public List<ComplaintTimeline> getTimeline() { return timeline; }
    public void setTimeline(List<ComplaintTimeline> timeline) { this.timeline = timeline; }

    public List<ComplaintAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<ComplaintAttachment> attachments) { this.attachments = attachments; }
}
