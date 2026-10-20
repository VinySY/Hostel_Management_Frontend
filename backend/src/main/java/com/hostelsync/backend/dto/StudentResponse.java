package com.hostelsync.backend.dto;

/**
 * Student data returned by the API — matches the frontend Student type exactly.
 *
 * Frontend fields:
 *   id, studentId, name, email, phone, gender, course, year,
 *   block, roomNumber, roomId, guardianName, guardianPhone, status
 */
public class StudentResponse {

    private String id;          // numeric DB id as string to match frontend "stu-1" style
    private String studentId;   // college ID, e.g. "STU-2023-089"
    private String name;
    private String email;
    private String phone;
    private String gender;
    private String course;
    private int year;
    private String block;
    private String roomNumber;
    private String roomId;       // numeric room DB id as string
    private String guardianName;
    private String guardianPhone;
    private String status;       // "Active" | "Inactive" | "Suspended"

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }

    public String getGuardianPhone() { return guardianPhone; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
