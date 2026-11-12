package com.hostelsync.backend.dto;

import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

public class WardenCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    private String phone;

    @NotNull(message = "Hostel ID is required")
    private Long hostelId;

    private List<Integer> assignedFloors = new ArrayList<>();

    public WardenCreateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getHostelId() { return hostelId; }
    public void setHostelId(Long hostelId) { this.hostelId = hostelId; }

    public List<Integer> getAssignedFloors() { return assignedFloors; }
    public void setAssignedFloors(List<Integer> assignedFloors) { this.assignedFloors = assignedFloors; }
}
