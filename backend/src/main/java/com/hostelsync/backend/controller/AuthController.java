package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.DemoLoginRequest;
import com.hostelsync.backend.dto.LoginRequest;
import com.hostelsync.backend.dto.LoginResponse;
import com.hostelsync.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/auth/login       — public endpoint, no JWT required
 * POST /api/auth/demo-login  — passwordless showcase demo endpoint (only when ENABLE_PUBLIC_DEMO=true)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/demo-login")
    public ResponseEntity<LoginResponse> demoLogin(@Valid @RequestBody DemoLoginRequest request) {
        LoginResponse response = userService.demoLogin(request);
        return ResponseEntity.ok(response);
    }

    /** Simple health check — useful for frontend connectivity tests */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("HostelSync backend is running");
    }
}
