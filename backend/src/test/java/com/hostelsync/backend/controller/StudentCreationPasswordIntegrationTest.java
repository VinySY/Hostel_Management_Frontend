package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.StudentCreateRequest;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.repository.StudentRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies removal of the default fallback student password ("studentPass2026"):
 * missing, blank, and short passwords must return HTTP 400 without creating a user/student,
 * and valid passwords are BCrypt-hashed and accepted for login.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:student_password_test;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=YEAR",
        "app.seeder.enabled=true",
        "app.bootstrap.enabled=false"
})
class StudentCreationPasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createStudent_missingOrBlankPassword_returnsHttp400AndDoesNotPersist() throws Exception {
        String wardenToken = login("warden.vance@hostelhub.edu", "wardenPass2026");

        // Missing password field
        Map<String, Object> missingPwPayload = baseStudentPayload("STU-PW-1", "missing.pw@hostelhub.edu");
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingPwPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Password is required")));

        assertFalse(studentRepository.existsByStudentId("STU-PW-1"));
        assertFalse(userRepository.existsByEmail("missing.pw@hostelhub.edu"));

        // Blank / whitespace password
        Map<String, Object> blankPwPayload = baseStudentPayload("STU-PW-2", "blank.pw@hostelhub.edu");
        blankPwPayload.put("password", "   ");
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankPwPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Password is required")));

        assertFalse(studentRepository.existsByStudentId("STU-PW-2"));
        assertFalse(userRepository.existsByEmail("blank.pw@hostelhub.edu"));

        // Short password (< 6 characters)
        Map<String, Object> shortPwPayload = baseStudentPayload("STU-PW-3", "short.pw@hostelhub.edu");
        shortPwPayload.put("password", "12345");
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPwPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("at least 6 characters")));

        assertFalse(studentRepository.existsByStudentId("STU-PW-3"));
    }

    @Test
    void createStudent_directServiceCallWithoutPassword_throwsBadRequest() {
        StudentCreateRequest req = new StudentCreateRequest();
        req.setName("Service Direct Student");
        req.setStudentId("STU-SVC-1");
        req.setEmail("svc.direct@hostelhub.edu");
        req.setPhone("+91 98765 00002");
        req.setCourse("B.Tech CS");
        req.setYear(2);
        req.setPassword("   ");

        assertThrows(BadRequestException.class, () -> studentService.createStudent(req));
        assertFalse(studentRepository.existsByStudentId("STU-SVC-1"));
        assertFalse(userRepository.existsByEmail("svc.direct@hostelhub.edu"));
    }

    @Test
    void createStudent_validPassword_hashesWithBcryptAndAllowsLogin() throws Exception {
        String wardenToken = login("warden.vance@hostelhub.edu", "wardenPass2026");
        String customPassword = "CustomStudent!Pass99";

        Map<String, Object> validPayload = baseStudentPayload("STU-PW-OK", "valid.student@hostelhub.edu");
        validPayload.put("password", customPassword);

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value("STU-PW-OK"));

        User createdUser = userRepository.findByEmail("valid.student@hostelhub.edu").orElseThrow();
        assertNotEquals(customPassword, createdUser.getPasswordHash());
        assertTrue(createdUser.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(customPassword, createdUser.getPasswordHash()));

        // Valid custom password logs in; removed default fallback does not
        String studentToken = login("valid.student@hostelhub.edu", customPassword);
        assertFalse(studentToken.isBlank());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "valid.student@hostelhub.edu",
                                "password", "studentPass2026"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    private Map<String, Object> baseStudentPayload(String studentId, String email) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Test Student");
        payload.put("studentId", studentId);
        payload.put("email", email);
        payload.put("phone", "+91 98765 11111");
        payload.put("course", "B.Tech CS");
        payload.put("year", 2);
        return payload;
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        return root.get("token").asText();
    }
}

