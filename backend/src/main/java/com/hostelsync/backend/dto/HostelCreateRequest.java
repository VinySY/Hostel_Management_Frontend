package com.hostelsync.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class HostelCreateRequest {

    @NotBlank(message = "Hostel name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 300)
    private String address;

    public HostelCreateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
