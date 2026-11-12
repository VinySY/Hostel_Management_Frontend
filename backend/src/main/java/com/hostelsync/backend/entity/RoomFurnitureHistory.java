package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Historical maintenance and replacement events for room furniture.
 */
@Entity
@Table(name = "room_furniture_history")
public class RoomFurnitureHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "furniture_type", nullable = false, length = 30)
    private FurnitureType furnitureType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private FurnitureActionType actionType;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 500)
    private String notes;

    @Column(name = "recorded_by", nullable = false)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }

    public RoomFurnitureHistory() {}

    public RoomFurnitureHistory(Room room, FurnitureType furnitureType, FurnitureActionType actionType,
                                int quantity, String notes, String recordedBy) {
        this.room = room;
        this.furnitureType = furnitureType;
        this.actionType = actionType;
        this.quantity = quantity;
        this.notes = notes;
        this.recordedBy = recordedBy;
        this.recordedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public FurnitureType getFurnitureType() { return furnitureType; }
    public void setFurnitureType(FurnitureType furnitureType) { this.furnitureType = furnitureType; }

    public FurnitureActionType getActionType() { return actionType; }
    public void setActionType(FurnitureActionType actionType) { this.actionType = actionType; }

    public int getQuantity() { return quantity; }
    public int getQuantityChanged() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
