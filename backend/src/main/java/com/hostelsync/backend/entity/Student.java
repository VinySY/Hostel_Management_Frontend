package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * Student resident profile. Linked 1-to-1 with a User account.
 * Room assignment is stored here (nullable — a student may not yet have a room).
 */
@Entity
@Table(name = "students",
       uniqueConstraints = @UniqueConstraint(columnNames = "student_id"))
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** One-to-one with the User login account */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** College-issued student identifier, e.g. "STU-2023-089" */
    @NotBlank
    @Size(max = 50)
    @Column(name = "student_id", nullable = false, unique = true)
    private String studentId;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false)
    private String course;

    @Min(1)
    @Max(6)
    @Column(nullable = false)
    private int year;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false)
    private String phone;

    @Size(max = 10)
    @Column(nullable = false)
    private String gender = "Male";

    @Size(max = 120)
    @Column(name = "guardian_name")
    private String guardianName;

    @Size(max = 20)
    @Column(name = "guardian_phone")
    private String guardianPhone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentStatus status = StudentStatus.Active;

    /** The room this student is assigned to (null = unallocated) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

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

    public Student() {}

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }

    public String getGuardianPhone() { return guardianPhone; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }

    public StudentStatus getStatus() { return status; }
    public void setStatus(StudentStatus status) { this.status = status; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
