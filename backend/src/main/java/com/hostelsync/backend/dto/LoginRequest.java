package com.hostelsync.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/auth/login
 */
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    public String getEmail() { return email; }

    /** Normalize pasted/autofilled emails before bean validation runs. */
    public void setEmail(String email) { this.email = email == null ? null : email.trim(); }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
