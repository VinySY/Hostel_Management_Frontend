package com.hostelsync.backend.dto;

import jakarta.validation.constraints.*;

public class RoomCreateRequest {

    @NotBlank(message = "Room number is required")
    @Size(max = 20)
    private String roomNumber;

    @NotBlank(message = "Block is required")
    @Size(max = 50)
    private String block;

    @Min(value = 0, message = "Floor must be 0 or greater")
    private int floor = 0;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 20, message = "Capacity cannot exceed 20")
    private int capacity = 2;

    @NotNull(message = "Hostel ID is required")
    private Long hostelId;

    /** "NONE" | "COOLER" | "AC" */
    private String coolingEquipment = "NONE";

    public RoomCreateRequest() {}

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public Long getHostelId() { return hostelId; }
    public void setHostelId(Long hostelId) { this.hostelId = hostelId; }

    public String getCoolingEquipment() { return coolingEquipment; }
    public void setCoolingEquipment(String coolingEquipment) { this.coolingEquipment = coolingEquipment; }
}
