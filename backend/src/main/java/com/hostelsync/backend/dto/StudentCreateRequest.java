package com.hostelsync.backend.dto;

import jakarta.validation.constraints.*;

/**
 * Request body for POST /api/students (warden creates a new student).
 */
public class StudentCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120)
    private String name;

    @NotBlank(message = "Student ID is required")
    @Size(max = 50)
    private String studentId;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    @Size(max = 200)
    private String email;

    /** Initial password set by the warden (will be BCrypt-hashed) */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Phone is required")
    @Size(max = 20)
    private String phone;

    @Size(max = 10)
    private String gender = "Male";

    @NotBlank(message = "Course is required")
    @Size(max = 150)
    private String course;

    @Min(value = 1, message = "Academic year must be between 1 and 6")
    @Max(value = 6, message = "Academic year must be between 1 and 6")
    private int year = 1;

    @Size(max = 120)
    private String guardianName;

    @Size(max = 20)
    private String guardianPhone;

    /** DB id of the room to assign (optional) */
    private Long roomId;

    /** "Active" | "Inactive" | "Suspended" */
    private String status = "Active";

    private String avatarUrl;

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }

    public String getGuardianPhone() { return guardianPhone; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
