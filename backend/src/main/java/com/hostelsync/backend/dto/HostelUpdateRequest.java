package com.hostelsync.backend.dto;

import jakarta.validation.constraints.Size;

public class HostelUpdateRequest {

    @Size(max = 150)
    private String name;

    @Size(max = 300)
    private String address;

    private Boolean active;

    public HostelUpdateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
