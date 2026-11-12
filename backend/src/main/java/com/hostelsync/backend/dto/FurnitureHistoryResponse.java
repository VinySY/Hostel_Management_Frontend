package com.hostelsync.backend.dto;

public class FurnitureHistoryResponse {

    private Long id;
    private Long roomId;
    private String roomNumber;
    private String furnitureType;
    private String actionType;
    private int quantity;
    private String notes;
    private String recordedBy;
    private String recordedAt;

    public FurnitureHistoryResponse() {}

    public FurnitureHistoryResponse(Long id, Long roomId, String roomNumber, String furnitureType,
                                    String actionType, int quantity, String notes,
                                    String recordedBy, String recordedAt) {
        this.id = id;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.furnitureType = furnitureType;
        this.actionType = actionType;
        this.quantity = quantity;
        this.notes = notes;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getFurnitureType() { return furnitureType; }
    public void setFurnitureType(String furnitureType) { this.furnitureType = furnitureType; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public String getRecordedAt() { return recordedAt; }
    public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }
}
