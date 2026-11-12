package com.hostelsync.backend.dto;

import java.time.LocalDateTime;

public class SurveyCommentDto {

    private Long id;
    private String studentName;
    private String studentId;
    private String hostelName;
    private String roomNumber;
    private String comments;
    private LocalDateTime submittedAt;

    public SurveyCommentDto() {}

    public SurveyCommentDto(Long id, String studentName, String studentId, String hostelName,
                            String roomNumber, String comments, LocalDateTime submittedAt) {
        this.id = id;
        this.studentName = studentName;
        this.studentId = studentId;
        this.hostelName = hostelName;
        this.roomNumber = roomNumber;
        this.comments = comments;
        this.submittedAt = submittedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getHostelName() { return hostelName; }
    public void setHostelName(String hostelName) { this.hostelName = hostelName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
