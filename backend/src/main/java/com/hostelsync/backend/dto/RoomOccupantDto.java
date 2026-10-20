package com.hostelsync.backend.dto;

/**
 * Compact student info embedded inside a RoomResponse occupants list.
 * Matches frontend RoomOccupant: { studentId, name, email, course, year, phone }
 */
public class RoomOccupantDto {

    private String studentId;
    private String name;
    private String email;
    private String course;
    private int year;
    private String phone;

    public RoomOccupantDto() {}

    public RoomOccupantDto(String studentId, String name, String email,
                           String course, int year, String phone) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.course = course;
        this.year = year;
        this.phone = phone;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
