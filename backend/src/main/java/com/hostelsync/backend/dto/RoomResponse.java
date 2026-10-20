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

    public List<RoomOccupantDto> getOccupants() { return occupants; }
    public void setOccupants(List<RoomOccupantDto> occupants) { this.occupants = occupants; }
}
