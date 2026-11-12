package com.hostelsync.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Passwordless public demo authentication request.
 */
public class DemoLoginRequest {

    @NotBlank(message = "Demo role is required")
    private String role;

    public DemoLoginRequest() {}

    public DemoLoginRequest(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

