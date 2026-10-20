package com.hostelsync.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Fields that can be updated on a student record.
 * All fields are optional — only non-null values are applied.
 */
public class StudentUpdateRequest {

    @Size(max = 20)
    private String phone;

    @Size(max = 120)
    private String guardianName;

    @Size(max = 20)
    private String guardianPhone;

    @Size(max = 150)
    private String course;

    private Integer year;

    private Long roomId;

    /** "Active" | "Inactive" | "Suspended" */
    private String status;

    @Size(max = 120)
    private String name;

    @Size(max = 10)
    private String gender;

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }

    public String getGuardianPhone() { return guardianPhone; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}
