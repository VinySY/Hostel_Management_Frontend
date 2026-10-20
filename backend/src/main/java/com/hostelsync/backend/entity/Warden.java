package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

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
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
