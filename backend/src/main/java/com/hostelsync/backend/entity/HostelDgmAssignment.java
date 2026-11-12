package com.hostelsync.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Maps a DGM to a specific hostel.
 * A DGM can be assigned to multiple hostels.
 */
@Entity
@Table(name = "hostel_dgm_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"hostel_id", "dgm_id"}))
public class HostelDgmAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dgm_id", nullable = false)
    private Dgm dgm;

    /** Who made this assignment */
    @Column(name = "assigned_by", nullable = false)
    private String assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }

    // -------- Constructors --------

    public HostelDgmAssignment() {}

    public HostelDgmAssignment(Hostel hostel, Dgm dgm, String assignedBy) {
        this.hostel = hostel;
        this.dgm = dgm;
        this.assignedBy = assignedBy;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Hostel getHostel() { return hostel; }
    public void setHostel(Hostel hostel) { this.hostel = hostel; }

    public Dgm getDgm() { return dgm; }
    public void setDgm(Dgm dgm) { this.dgm = dgm; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
}
