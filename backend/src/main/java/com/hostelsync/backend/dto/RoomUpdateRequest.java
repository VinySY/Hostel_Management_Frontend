package com.hostelsync.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class RoomUpdateRequest {

    @Size(max = 50)
    private String block;

    @Min(value = 0, message = "Floor must be 0 or greater")
    private Integer floor;

    @Min(value = 1, message = "Capacity must be between 1 and 20")
    @Max(value = 20, message = "Capacity must be between 1 and 20")
    private Integer capacity;

    private Boolean active;

    /** "NONE" | "COOLER" | "AC" */
    private String coolingEquipment;

    /** "OPERATIONAL" | "MAINTENANCE_REQUIRED" | "REPLACEMENT_NEEDED" */
    private String coolingMaintenanceStatus;

    public RoomUpdateRequest() {}

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getCoolingEquipment() { return coolingEquipment; }
    public void setCoolingEquipment(String coolingEquipment) { this.coolingEquipment = coolingEquipment; }

    public String getCoolingMaintenanceStatus() { return coolingMaintenanceStatus; }
    public void setCoolingMaintenanceStatus(String coolingMaintenanceStatus) { this.coolingMaintenanceStatus = coolingMaintenanceStatus; }
}
