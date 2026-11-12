package com.hostelsync.backend.dto;

import com.hostelsync.backend.entity.User;

/**
 * Response for DGM account information.
 */
public class DgmResponse {

    private Long id;
    private String name;
    private String email;
    private String employeeId;
    private Long userId;

    public DgmResponse() {}

    public DgmResponse(Long id, String name, String email, String employeeId, Long userId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.employeeId = employeeId;
        this.userId = userId;
    }

    // Factory method from User entity
    public static DgmResponse fromUserAndDgmId(User user, Long dgmId, String employeeId) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return new DgmResponse(
                dgmId,
                user.getName(),
                user.getEmail(),
                employeeId,
                user.getId()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "DgmResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", userId=" + userId +
                '}';
    }
}