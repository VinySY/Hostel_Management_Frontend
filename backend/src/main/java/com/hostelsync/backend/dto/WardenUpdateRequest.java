package com.hostelsync.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class WardenUpdateRequest {

    private String name;
    private String employeeId;
    private String phone;
    private Long hostelId;
    private Boolean active;
    private List<Integer> assignedFloors;

    public WardenUpdateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getHostelId() { return hostelId; }
    public void setHostelId(Long hostelId) { this.hostelId = hostelId; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<Integer> getAssignedFloors() { return assignedFloors; }
    public void setAssignedFloors(List<Integer> assignedFloors) { this.assignedFloors = assignedFloors; }
}
