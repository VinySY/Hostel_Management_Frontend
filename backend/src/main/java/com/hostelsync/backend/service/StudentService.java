package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.StudentCreateRequest;
import com.hostelsync.backend.dto.StudentResponse;
import com.hostelsync.backend.dto.StudentUpdateRequest;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ConflictException;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.StudentRepository;
import com.hostelsync.backend.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoomService roomService;
    private final MappingService mappingService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final FileStorageService fileStorageService;

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          RoomService roomService,
                          MappingService mappingService,
                          PasswordEncoder passwordEncoder,
                          AuditService auditService,
                          FileStorageService fileStorageService) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.roomService = roomService;
        this.mappingService = mappingService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.fileStorageService = fileStorageService;
    }

    // ─── List all students ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(mappingService::toStudentResponse)
                .toList();
    }

    // ─── Get by numeric DB id ─────────────────────────────────────────────────

    // ─── Get current authenticated student profile ───────────────────────────

    @Transactional(readOnly = true)
    public StudentResponse getCurrentStudentProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        Student student = findEntityByUser(user);
        return mappingService.toStudentResponse(student);
    }

    // ─── Get by numeric DB id ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        Student student = findEntityById(id);
        return mappingService.toStudentResponse(student);
    }

    // ─── Get by college student id string ────────────────────────────────────

    @Transactional(readOnly = true)
    public StudentResponse getStudentByStudentId(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with studentId: " + studentId));
        return mappingService.toStudentResponse(student);
    }

    // ─── Create (warden registers a new student) ──────────────────────────────

    public StudentResponse createStudent(StudentCreateRequest req) {
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        String rawPassword = req.getPassword();
        if (rawPassword.length() < 6 || rawPassword.length() > 100) {
            throw new BadRequestException("Password must be between 6 and 100 characters");
        }

        // Uniqueness checks
        if (studentRepository.existsByStudentId(req.getStudentId())) {
            throw new ConflictException("Student ID already exists: " + req.getStudentId());
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("Email already registered: " + req.getEmail());
        }

        // Create the login account
        User user = new User(req.getName(), req.getEmail(),
                passwordEncoder.encode(rawPassword), UserRole.STUDENT);
        if (req.getAvatarUrl() != null && !req.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(req.getAvatarUrl());
        }
        user = userRepository.save(user);

        // Build the student profile
        validateYear(req.getYear());
        Student student = new Student();
        student.setUser(user);
        student.setStudentId(req.getStudentId());
        student.setCourse(req.getCourse());
        student.setYear(req.getYear());
        student.setPhone(req.getPhone());
        student.setGender(req.getGender() != null ? req.getGender() : "Male");
        student.setGuardianName(req.getGuardianName());
        student.setGuardianPhone(req.getGuardianPhone());
        student.setStatus(StudentStatus.valueOf(
                req.getStatus() != null ? req.getStatus() : "Active"));

        // Room assignment
        if (req.getRoomId() != null) {
            Room room = roomService.findEntityById(req.getRoomId());
            if (!room.isActive()) {
                throw new BadRequestException("Room is not active and cannot accept students.");
            }
            if (!roomService.hasCapacity(room)) {
                throw new BadRequestException("Room " + room.getRoomNumber() + " is already at full capacity.");
            }
            student.setRoom(room);
        }

        student = studentRepository.save(student);
        auditService.log("Student", student.getId(), "STUDENT_REGISTERED", user,
                null, student.getStudentId(), "New student registered by warden", null);
        return mappingService.toStudentResponse(student);
    }

    // ─── Update (warden-only official profile edit) ──────────────────────────

    public StudentResponse updateStudent(Long id, StudentUpdateRequest req,
                                         boolean callerIsWarden, String callerEmail) {
        Student student = findEntityById(id);
        User caller = userRepository.findByEmail(callerEmail).orElse(null);

        // Section 8: Students cannot edit official profile fields — warden only
        if (!callerIsWarden) {
            throw new ForbiddenException("Students are not permitted to modify official profile fields. Please contact your warden.");
        }

        User user = student.getUser();
        String previousSummary = student.getStudentId() + " (" + user.getName() + ")";
        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }
        userRepository.save(user);

        if (req.getPhone() != null)        student.setPhone(req.getPhone());
        if (req.getGender() != null)       student.setGender(req.getGender());
        if (req.getCourse() != null)       student.setCourse(req.getCourse());
        if (req.getYear() != null) {
            validateYear(req.getYear());
            student.setYear(req.getYear());
        }
        if (req.getGuardianName() != null) student.setGuardianName(req.getGuardianName());
        if (req.getGuardianPhone() != null) student.setGuardianPhone(req.getGuardianPhone());
        if (req.getStatus() != null) {
            student.setStatus(StudentStatus.valueOf(req.getStatus()));
        }

        if (req.getRoomId() != null) {
            Room room = roomService.findEntityById(req.getRoomId());
            if (!room.isActive()) {
                throw new BadRequestException("Room is not active.");
            }
            // Only enforce capacity if assigning to a different room
            if (student.getRoom() == null || !student.getRoom().getId().equals(room.getId())) {
                if (!roomService.hasCapacity(room)) {
                    throw new BadRequestException("Room " + room.getRoomNumber() + " is at full capacity.");
                }
            }
            student.setRoom(room);
        }

        student = studentRepository.save(student);
        if (caller != null) {
            auditService.log("Student", student.getId(), "PROFILE_UPDATED", caller,
                    previousSummary, student.getStudentId() + " (" + user.getName() + ")",
                    "Warden updated student profile", null);
        }
        return mappingService.toStudentResponse(student);
    }

    // ─── Upload Profile Photo (warden-only) ──────────────────────────────────

    public StudentResponse uploadStudentPhoto(Long id, MultipartFile file, User caller) {
        Student student = findEntityById(id);
        String previousAvatar = student.getUser().getAvatarUrl();

        String storedFileName = fileStorageService.store(file);
        String avatarUrl = "/api/files/" + storedFileName;

        User user = student.getUser();
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        auditService.log("Student", student.getId(), "PHOTO_UPLOADED", caller,
                previousAvatar, avatarUrl, "Warden uploaded profile photo", null);

        return mappingService.toStudentResponse(student);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deleteStudent(Long id) {
        Student student = findEntityById(id);
        studentRepository.delete(student);
        // The associated User is NOT auto-deleted — that would be done separately
        // if needed (e.g. deactivate the login account instead).
        userRepository.delete(student.getUser());
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private static void validateYear(int year) {
        if (year < 1 || year > 6) {
            throw new BadRequestException("Academic year must be between 1 and 6");
        }
    }

    public Student findEntityById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    public Student findEntityByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student with studentId: " + studentId));
    }

    public Student findEntityByUser(User user) {
        return studentRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for this user"));
    }
}
