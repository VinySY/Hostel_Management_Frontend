package com.hostelsync.backend.dto;

/**
 * Response for POST /api/auth/login — matches frontend AuthResponse shape:
 * { token, user: { id, name, email, role, studentId?, avatar? } }
 */
public class LoginResponse {

    private String token;
    private UserDto user;

    public LoginResponse(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public UserDto getUser() { return user; }

    // ─── Nested user DTO (serialises to the "user" field) ────────────────────

    public static class UserDto {
        private String id;       // String to match frontend ("usr-101" style or numeric as string)
        private String name;
        private String email;
        private String role;     // "STUDENT" | "WARDEN"
        private String studentId; // only populated for students
        private String avatar;

        public UserDto(String id, String name, String email, String role,
                       String studentId, String avatar) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.studentId = studentId;
            this.avatar = avatar;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getStudentId() { return studentId; }
        public String getAvatar() { return avatar; }
    }
}
