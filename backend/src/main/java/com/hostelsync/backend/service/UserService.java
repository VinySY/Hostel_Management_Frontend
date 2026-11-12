package com.hostelsync.backend.service;

import com.hostelsync.backend.config.DemoAccountRegistry;
import com.hostelsync.backend.dto.DemoLoginRequest;
import com.hostelsync.backend.dto.LoginRequest;
import com.hostelsync.backend.dto.LoginResponse;
import com.hostelsync.backend.entity.Student;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.repository.StudentRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final DemoAccountRegistry demoAccountRegistry;

    @Autowired
    public UserService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder,
                       DemoAccountRegistry demoAccountRegistry) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.demoAccountRegistry = demoAccountRegistry;
    }

    /**
     * Authenticates a user and returns a JWT + user info.
     */
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim();

        // Prevent password login against demo identities when public demo mode is disabled
        if (demoAccountRegistry.isDemoEmail(email) && !demoAccountRegistry.isPublicDemoEnabled()) {
            throw new ForbiddenException("Public demo mode is disabled.");
        }

        // Spring Security does the credential check; throws AuthenticationException on failure
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        boolean isDemo = demoAccountRegistry.isDemoEmail(user.getEmail());
        return buildLoginResponse(user, isDemo);
    }

    /**
     * Passwordless demo authentication issuing a read-only demo-scoped JWT
     * only when ENABLE_PUBLIC_DEMO=true.
     */
    public LoginResponse demoLogin(DemoLoginRequest request) {
        if (!demoAccountRegistry.isPublicDemoEnabled()) {
            throw new ForbiddenException("Public demo mode is disabled.");
        }

        String rawRole = request != null && request.getRole() != null ? request.getRole().trim() : "";
        if (rawRole.isEmpty()) {
            throw new BadRequestException("Demo role is required");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(rawRole.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported demo role: " + rawRole + ". Must be STUDENT, WARDEN, or DGM.");
        }

        String demoEmail = demoAccountRegistry.getDemoEmailForRole(role)
                .orElseThrow(() -> new BadRequestException("Demo account is not configured for role: " + role.name()));

        User user = userRepository.findByEmailIgnoreCase(demoEmail)
                .orElseThrow(() -> new BadRequestException("Demo sandbox accounts are not initialized."));

        if (user.getRole() != role) {
            throw new BadRequestException("Configured demo user role mismatch.");
        }

        return buildLoginResponse(user, true);
    }

    private LoginResponse buildLoginResponse(User user, boolean demo) {
        String token = jwtUtil.generateToken(user.getEmail(), demo);

        // Populate studentId for student accounts
        String studentId = null;
        if (user.getRole() == UserRole.STUDENT) {
            Optional<Student> studentOpt = studentRepository.findByUser(user);
            if (studentOpt.isPresent()) {
                studentId = studentOpt.get().getStudentId();
            }
        }

        LoginResponse.UserDto userDto = new LoginResponse.UserDto(
                String.valueOf(user.getId()),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                studentId,
                user.getAvatarUrl(),
                demo
        );

        return new LoginResponse(token, userDto);
    }

    /**
     * Create a User record (called by StudentService / seeder).
     * Does NOT create a Student profile — that is done separately.
     */
    public User createUser(String name, String email, String plainPassword, UserRole role) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("A user with this email already exists: " + email);
        }
        User user = new User(name, email, passwordEncoder.encode(plainPassword), role);
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
