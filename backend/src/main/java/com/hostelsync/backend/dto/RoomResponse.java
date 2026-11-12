package com.hostelsync.backend.dto;

import java.util.List;

/**
 * Room data returned by the API — matches the frontend Room type:
 *   id, roomNumber, block, floor, capacity, occupied, status, occupants[]
 *
 * "status" is derived: "Available" | "Partially Occupied" | "Full"
 * "occupied" is the count of active students in this room.
 */
public class RoomResponse {

    private String id;
    private String roomNumber;
    private String block;
    private int floor;
    private int capacity;
    private int occupied;
    private String status;          // "Available" | "Partially Occupied" | "Full"
    private boolean active = true;
    private String hostelId;
    private String hostelName;
    private String coolingEquipment;           // "NONE" | "COOLER" | "AC"
    private String coolingMaintenanceStatus;   // "OPERATIONAL" | "MAINTENANCE_REQUIRED" | "REPLACEMENT_NEEDED"
    private List<RoomFurnitureDto> furnitureInventory;
    private boolean hasMaintenanceIssues;
    private boolean hasShortage;
    private List<RoomOccupantDto> occupants;

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getOccupied() { return occupied; }
    public void setOccupied(int occupied) { this.occupied = occupied; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getHostelId() { return hostelId; }
    public void setHostelId(String hostelId) { this.hostelId = hostelId; }

    public String getHostelName() { return hostelName; }
    public void setHostelName(String hostelName) { this.hostelName = hostelName; }

    public String getCoolingEquipment() { return coolingEquipment; }
    public void setCoolingEquipment(String coolingEquipment) { this.coolingEquipment = coolingEquipment; }

    public String getCoolingMaintenanceStatus() { return coolingMaintenanceStatus; }
    public void setCoolingMaintenanceStatus(String coolingMaintenanceStatus) { this.coolingMaintenanceStatus = coolingMaintenanceStatus; }

    public List<RoomFurnitureDto> getFurnitureInventory() { return furnitureInventory; }
    public void setFurnitureInventory(List<RoomFurnitureDto> furnitureInventory) { this.furnitureInventory = furnitureInventory; }

    public boolean isHasMaintenanceIssues() { return hasMaintenanceIssues; }
    public void setHasMaintenanceIssues(boolean hasMaintenanceIssues) { this.hasMaintenanceIssues = hasMaintenanceIssues; }

    public boolean isHasShortage() { return hasShortage; }
    public void setHasShortage(boolean hasShortage) { this.hasShortage = hasShortage; }

    public List<RoomOccupantDto> getOccupants() { return occupants; }
    public void setOccupants(List<RoomOccupantDto> occupants) { this.occupants = occupants; }
}
