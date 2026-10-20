package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Metadata about an uploaded file attached to a complaint.
 * The actual file bytes are stored on disk (not in the database).
 */
@Entity
@Table(name = "complaint_attachments")
public class ComplaintAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    /** The filename the user originally uploaded (display only, never used for storage path) */
    @NotBlank
    @Size(max = 255)
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    /** The UUID-based safe filename stored on disk, e.g. "a3f8c1d2-photo.jpg" */
    @NotBlank
    @Size(max = 255)
    @Column(name = "stored_file_name", nullable = false, unique = true)
    private String storedFileName;

    /** MIME type, e.g. "image/jpeg" */
    @Size(max = 100)
    @Column(name = "content_type")
    private String contentType;

    /** File size in bytes */
    @Positive
    @Column(name = "file_size")
    private long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // -------- Constructors --------

    public ComplaintAttachment() {}

    public ComplaintAttachment(Complaint complaint, String originalFileName,
                               String storedFileName, String contentType, long fileSize) {
        this.complaint = complaint;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
