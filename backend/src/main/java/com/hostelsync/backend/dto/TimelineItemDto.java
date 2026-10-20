package com.hostelsync.backend.dto;

/**
 * A single timeline entry for a complaint.
 * Matches frontend ComplaintTimelineItem: { status, timestamp, note? }
 */
public class TimelineItemDto {

    private String status;     // display string e.g. "Under Review"
    private String timestamp;  // ISO-8601
    private String note;

    public TimelineItemDto() {}

    public TimelineItemDto(String status, String timestamp, String note) {
        this.status = status;
        this.timestamp = timestamp;
        this.note = note;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
