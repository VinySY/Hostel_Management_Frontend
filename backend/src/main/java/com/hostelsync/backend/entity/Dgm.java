package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DGM (Deputy General Manager) profile record. Linked 1-to-1 with a User account.
 * A DGM is assigned to one or more hostels via the HostelDgmAssignment entity.
 */
@Entity
@Table(name = "dgms")
public class Dgm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Internal employee identifier, e.g. "DGM-0042" */
    @Size(max = 50)
    @Column(name = "employee_id")
    private String employeeId;

    /** DGM's department or designation */
    @Size(max = 150)
    @Column(name = "designation")
    private String designation;

    /** DGM's office location or building */
    @Size(max = 150)
    @Column(name = "office_location")
    private String officeLocation;

    /** DGM's contact extension */
    @Size(max = 20)
    @Column(name = "contact_extension")
    private String contactExtension;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Hostels assigned to this DGM */
    @OneToMany(mappedBy = "dgm", fetch = FetchType.LAZY)
    private List<HostelDgmAssignment> hostelAssignments = new ArrayList<>();

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

    public Dgm() {}

    public Dgm(User user, String employeeId, String designation) {
        this.user = user;
        this.employeeId = employeeId;
        this.designation = designation;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getOfficeLocation() { return officeLocation; }
    public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }

    public String getContactExtension() { return contactExtension; }
    public void setContactExtension(String contactExtension) { this.contactExtension = contactExtension; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public List<HostelDgmAssignment> getHostelAssignments() { return hostelAssignments; }
    public void setHostelAssignments(List<HostelDgmAssignment> hostelAssignments) { this.hostelAssignments = hostelAssignments; }
}