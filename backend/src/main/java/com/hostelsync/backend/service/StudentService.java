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

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          RoomService roomService,
                          MappingService mappingService,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.roomService = roomService;
        this.mappingService = mappingService;
        this.passwordEncoder = passwordEncoder;
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
        // Uniqueness checks
        if (studentRepository.existsByStudentId(req.getStudentId())) {
            throw new ConflictException("Student ID already exists: " + req.getStudentId());
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("Email already registered: " + req.getEmail());
        }

        // Default password if not provided
        String rawPassword = (req.getPassword() != null && !req.getPassword().isBlank())
                ? req.getPassword()
                : "studentPass2026";

        // Create the login account
        User user = new User(req.getName(), req.getEmail(),
                passwordEncoder.encode(rawPassword), UserRole.STUDENT);
        user = userRepository.save(user);

        // Build the student profile
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
        return mappingService.toStudentResponse(student);
    }

    // ─── Update (warden or student updating own profile) ─────────────────────

    public StudentResponse updateStudent(Long id, StudentUpdateRequest req,
                                         boolean callerIsWarden, String callerEmail) {
        Student student = findEntityById(id);

        // Students can only update their own record
        if (!callerIsWarden && !student.getUser().getEmail().equals(callerEmail)) {
            throw new ForbiddenException("You can only update your own profile.");
        }

        // Students cannot change room assignment or status — only wardens can
        if (!callerIsWarden) {
            if (req.getRoomId() != null || req.getStatus() != null) {
                throw new ForbiddenException("Only a warden can change room assignment or account status.");
            }
        }

        User user = student.getUser();
        if (req.getName() != null)         user.setName(req.getName());
        userRepository.save(user);

        if (req.getPhone() != null)        student.setPhone(req.getPhone());
        if (req.getGender() != null)       student.setGender(req.getGender());
        if (req.getCourse() != null)       student.setCourse(req.getCourse());
        if (req.getYear() != null)         student.setYear(req.getYear());
        if (req.getGuardianName() != null) student.setGuardianName(req.getGuardianName());
        if (req.getGuardianPhone() != null) student.setGuardianPhone(req.getGuardianPhone());

        if (req.getStatus() != null && callerIsWarden) {
            student.setStatus(StudentStatus.valueOf(req.getStatus()));
        }

        if (req.getRoomId() != null && callerIsWarden) {
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
