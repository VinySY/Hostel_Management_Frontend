package com.hostelsync.backend.dto;

import java.util.List;

/**
 * Complaint data returned by the API — matches the frontend Complaint type exactly.
 *
 * Frontend fields:
 *   id, complaintNumber, studentId, studentName, roomId, roomNumber, block,
 *   type, subject, description, location, priority, status, adminNotes,
 *   attachments[], timeline[], createdAt, updatedAt
 */
public class ComplaintResponse {

    private String id;
    private String complaintNumber;
    private String studentId;      // college id like "STU-2023-089"
    private String studentName;
    private String roomId;
    private String roomNumber;
    private String block;
    private String type;           // display string e.g. "Room Maintenance"
    private String subject;
    private String description;
    private String location;
    private String priority;       // "Low" | "Medium" | "High" | "Urgent"
    private String status;         // "Submitted" | "Under Review" | ...
    private String adminNotes;
    private String hostelId;
    private String hostelName;
    private Integer floor;
    private boolean escalated;
    private boolean slaBreached;
    private String slaDeadline;
    private String overdueAt;
    private String escalatedAt;
    private List<AttachmentDto> attachments;
    private List<TimelineItemDto> timeline;
    private String createdAt;
    private String updatedAt;

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getComplaintNumber() { return complaintNumber; }
    public void setComplaintNumber(String complaintNumber) { this.complaintNumber = complaintNumber; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getHostelId() { return hostelId; }
    public void setHostelId(String hostelId) { this.hostelId = hostelId; }

    public String getHostelName() { return hostelName; }
    public void setHostelName(String hostelName) { this.hostelName = hostelName; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public boolean isEscalated() { return escalated; }
    public void setEscalated(boolean escalated) { this.escalated = escalated; }

    public boolean isSlaBreached() { return slaBreached; }
    public void setSlaBreached(boolean slaBreached) { this.slaBreached = slaBreached; }

    public String getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(String slaDeadline) { this.slaDeadline = slaDeadline; }

    public String getOverdueAt() { return overdueAt; }
    public void setOverdueAt(String overdueAt) { this.overdueAt = overdueAt; }

    public String getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(String escalatedAt) { this.escalatedAt = escalatedAt; }

    public List<AttachmentDto> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentDto> attachments) { this.attachments = attachments; }

    public List<TimelineItemDto> getTimeline() { return timeline; }
    public void setTimeline(List<TimelineItemDto> timeline) { this.timeline = timeline; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
