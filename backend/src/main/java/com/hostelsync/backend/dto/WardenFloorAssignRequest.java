package com.hostelsync.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class WardenFloorAssignRequest {

    @NotNull(message = "Hostel ID is required")
    private Long hostelId;

    private List<Integer> floors = new ArrayList<>();

    public WardenFloorAssignRequest() {}

    public Long getHostelId() { return hostelId; }
    public void setHostelId(Long hostelId) { this.hostelId = hostelId; }

    public List<Integer> getFloors() { return floors; }
    public void setFloors(List<Integer> floors) { this.floors = floors; }
}
