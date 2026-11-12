package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Warden profile record. Linked 1-to-1 with a User account.
 */
@Entity
@Table(name = "wardens")
public class Warden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Internal employee identifier, e.g. "WDN-0042" */
    @Size(max = 50)
    @Column(name = "employee_id")
    private String employeeId;

    @Size(max = 30)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id")
    private Hostel hostel;

    @OneToMany(mappedBy = "warden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WardenFloorAssignment> floorAssignments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // -------- Constructors --------

    public Warden() {}

    public Warden(User user, String employeeId) {
        this.user = user;
        this.employeeId = employeeId;
        this.active = true;
    }

    public Warden(User user, String employeeId, Hostel hostel, String phone) {
        this.user = user;
        this.employeeId = employeeId;
        this.hostel = hostel;
        this.phone = phone;
        this.active = true;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Hostel getHostel() { return hostel; }
    public void setHostel(Hostel hostel) { this.hostel = hostel; }

    public List<WardenFloorAssignment> getFloorAssignments() { return floorAssignments; }
    public void setFloorAssignments(List<WardenFloorAssignment> floorAssignments) { this.floorAssignments = floorAssignments; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

