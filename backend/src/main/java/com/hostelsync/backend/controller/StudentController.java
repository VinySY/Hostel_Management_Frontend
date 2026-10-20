package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.StudentCreateRequest;
import com.hostelsync.backend.dto.StudentResponse;
import com.hostelsync.backend.dto.StudentUpdateRequest;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for student management.
 *
 * GET    /api/students            — warden: list all students
 * POST   /api/students            — warden: register new student
 * GET    /api/students/{id}       — authenticated: get student by DB id
 * PUT    /api/students/{id}       — authenticated: update student (students limited to own record)
 * DELETE /api/students/{id}       — warden: remove student
 *
 * Students can also look themselves up by their college ID:
 * GET    /api/students/by-student-id/{studentId}
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final UserRepository userRepository;

    public StudentController(StudentService studentService, UserRepository userRepository) {
        this.studentService = studentService;
        this.userRepository = userRepository;
    }

    // ─── List all (warden only — enforced in SecurityConfig) ─────────────────

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    // ─── Create (warden only) ─────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody StudentCreateRequest request) {
        StudentResponse created = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── Get by DB id ─────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        // Students may only fetch their own record
        User caller = findUser(principal.getUsername());
        if (caller.getRole() == UserRole.STUDENT) {
            // Look up the student to verify ownership
            StudentResponse resp = studentService.getStudentById(id);
            if (!resp.getEmail().equals(caller.getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(resp);
        }

        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    // ─── Get by college student ID string ────────────────────────────────────

    @GetMapping("/by-student-id/{studentId}")
    public ResponseEntity<StudentResponse> getByStudentId(
            @PathVariable String studentId,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());
        StudentResponse resp = studentService.getStudentByStudentId(studentId);

        // Students can only see their own profile
        if (caller.getRole() == UserRole.STUDENT && !resp.getEmail().equals(caller.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(resp);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());
        boolean isWarden = caller.getRole() == UserRole.WARDEN;

        StudentResponse updated = studentService.updateStudent(id, request, isWarden, caller.getEmail());
        return ResponseEntity.ok(updated);
    }

    // ─── Delete (warden only) ─────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }
}
