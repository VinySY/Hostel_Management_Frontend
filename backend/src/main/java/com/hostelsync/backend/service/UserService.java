package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.LoginRequest;
import com.hostelsync.backend.dto.LoginResponse;
import com.hostelsync.backend.entity.Student;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.repository.StudentRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user and returns a JWT + user info.
     */
    public LoginResponse login(LoginRequest request) {
        // Spring Security does the credential check; throws AuthenticationException on failure
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail());

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
                user.getAvatarUrl()
        );

        return new LoginResponse(token, userDto);
    }

    /**
     * Create a User record (called by StudentService / seeder).
     * Does NOT create a Student profile — that is done separately.
     */
    public User createUser(String name, String email, String plainPassword, UserRole role) {
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
