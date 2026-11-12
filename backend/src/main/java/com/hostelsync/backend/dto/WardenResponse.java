package com.hostelsync.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class WardenResponse {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String employeeId;
    private String phone;
    private Long hostelId;
    private String hostelName;
    private boolean active;
    private List<Integer> assignedFloors = new ArrayList<>();
    private long activeComplaintsCount;
    private long resolvedComplaintsCount;

    public WardenResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getHostelId() { return hostelId; }
    public void setHostelId(Long hostelId) { this.hostelId = hostelId; }

    public String getHostelName() { return hostelName; }
    public void setHostelName(String hostelName) { this.hostelName = hostelName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<Integer> getAssignedFloors() { return assignedFloors; }
    public void setAssignedFloors(List<Integer> assignedFloors) { this.assignedFloors = assignedFloors; }

    public long getActiveComplaintsCount() { return activeComplaintsCount; }
    public void setActiveComplaintsCount(long activeComplaintsCount) { this.activeComplaintsCount = activeComplaintsCount; }

    public long getResolvedComplaintsCount() { return resolvedComplaintsCount; }
    public void setResolvedComplaintsCount(long resolvedComplaintsCount) { this.resolvedComplaintsCount = resolvedComplaintsCount; }
}
