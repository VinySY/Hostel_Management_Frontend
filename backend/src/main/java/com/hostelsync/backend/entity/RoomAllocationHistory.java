package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * History of room allocations, transfers, and departures.
 * Provides an audit trail for student room assignments.
 */
@Entity
@Table(name = "room_allocation_history",
       indexes = {
           @Index(columnList = "student_id"),
           @Index(columnList = "room_id"),
           @Index(columnList = "action_timestamp")
       })
public class RoomAllocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "room_id")
    private Long roomId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllocationAction action;

    /** Who performed this action */
    @NotNull
    @Column(name = "performed_by_id", nullable = false)
    private Long performedById;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "action_timestamp", nullable = false, updatable = false)
    private LocalDateTime actionTimestamp;

    @PrePersist
    protected void onCreate() {
        actionTimestamp = LocalDateTime.now();
    }

    // -------- Constructors --------

    public RoomAllocationHistory() {}

    public RoomAllocationHistory(Long studentId, Long roomId, AllocationAction action, Long performedById) {
        this.studentId = studentId;
        this.roomId = roomId;
        this.action = action;
        this.performedById = performedById;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public AllocationAction getAction() { return action; }
    public void setAction(AllocationAction action) { this.action = action; }

    public Long getPerformedById() { return performedById; }
    public void setPerformedById(Long performedById) { this.performedById = performedById; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getActionTimestamp() { return actionTimestamp; }
}
