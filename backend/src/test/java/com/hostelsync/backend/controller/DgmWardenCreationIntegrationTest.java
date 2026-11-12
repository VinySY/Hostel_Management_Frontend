package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.WardenCreateRequest;
import com.hostelsync.backend.entity.Hostel;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.repository.HostelRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.repository.WardenRepository;
import com.hostelsync.backend.service.DgmWardenService;
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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies POST /api/dgm/wardens and DgmWardenService password validation,
 * BCrypt hashing, non-leakage in responses, and existing warden creation behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dgm_warden_creation_test;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=YEAR",
        "app.seeder.enabled=true",
        "app.bootstrap.enabled=false"
})
class DgmWardenCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WardenRepository wardenRepository;

    @Autowired
    private DgmWardenService dgmWardenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createWarden_validPassword_storesBcryptHashPreservesFieldsAndNeverExposesPassword() throws Exception {
        String dgmToken = login("dgm.director@hostelhub.edu", "dgmPass2026");
        Hostel hostel = hostelRepository.findAll().get(0);
        String rawPassword = "WardenProd!StrongPass2026";

        Map<String, Object> payload = basePayload("WAR-VALID-1", "warden.valid@hostelhub.edu", hostel.getId());
        payload.put("password", rawPassword);

        String responseBody = mockMvc.perform(post("/api/dgm/wardens")
                        .header("Authorization", "Bearer " + dgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value("WAR-VALID-1"))
                .andExpect(jsonPath("$.name").value("New Test Warden"))
                .andExpect(jsonPath("$.email").value("warden.valid@hostelhub.edu"))
                .andExpect(jsonPath("$.hostelId").value(hostel.getId()))
                .andExpect(jsonPath("$.assignedFloors[0]").value(1))
                .andExpect(jsonPath("$.assignedFloors[1]").value(2))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(responseBody.contains(rawPassword));

        User savedUser = userRepository.findByEmail("warden.valid@hostelhub.edu").orElseThrow();
        assertNotEquals(rawPassword, savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(rawPassword, savedUser.getPasswordHash()));

        // Verify created warden can authenticate with supplied password
        String wardenToken = login("warden.valid@hostelhub.edu", rawPassword);
        assertFalse(wardenToken.isBlank());
    }

    @Test
    void createWarden_missingBlankShortAndTooLongPasswords_rejectedWithHttp400() throws Exception {
        String dgmToken = login("dgm.director@hostelhub.edu", "dgmPass2026");
        Hostel hostel = hostelRepository.findAll().get(0);

        // Missing password
        Map<String, Object> missingPayload = basePayload("WAR-MISS-1", "warden.miss@hostelhub.edu", hostel.getId());
        mockMvc.perform(post("/api/dgm/wardens")
                        .header("Authorization", "Bearer " + dgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Password is required")));
        assertFalse(userRepository.existsByEmail("warden.miss@hostelhub.edu"));
        assertTrue(wardenRepository.findByEmployeeId("WAR-MISS-1").isEmpty());

        // Blank / whitespace password
        Map<String, Object> blankPayload = basePayload("WAR-BLANK-1", "warden.blank@hostelhub.edu", hostel.getId());
        blankPayload.put("password", "     ");
        mockMvc.perform(post("/api/dgm/wardens")
                        .header("Authorization", "Bearer " + dgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Password is required")));
        assertFalse(userRepository.existsByEmail("warden.blank@hostelhub.edu"));
        assertTrue(wardenRepository.findByEmployeeId("WAR-BLANK-1").isEmpty());

        // Too-short password (< 6)
        Map<String, Object> shortPayload = basePayload("WAR-SHORT-1", "warden.short@hostelhub.edu", hostel.getId());
        shortPayload.put("password", "12345");
        mockMvc.perform(post("/api/dgm/wardens")
                        .header("Authorization", "Bearer " + dgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("between 6 and 100 characters")));
        assertFalse(userRepository.existsByEmail("warden.short@hostelhub.edu"));

        // Too-long password (> 100)
        Map<String, Object> longPayload = basePayload("WAR-LONG-1", "warden.long@hostelhub.edu", hostel.getId());
        longPayload.put("password", "x".repeat(101));
        mockMvc.perform(post("/api/dgm/wardens")
                        .header("Authorization", "Bearer " + dgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("between 6 and 100 characters")));
        assertFalse(userRepository.existsByEmail("warden.long@hostelhub.edu"));
    }

    @Test
    void createWarden_serviceValidationAndDgmRoleAuthorizationPreserved() throws Exception {
        Hostel hostel = hostelRepository.findAll().get(0);

        WardenCreateRequest blankReq = new WardenCreateRequest();
        blankReq.setEmployeeId("WAR-SVC-1");
        blankReq.setName("Service Warden");
        blankReq.setEmail("warden.svc@hostelhub.edu");
        blankReq.setHostelId(hostel.getId());
        blankReq.setPassword("   ");
        assertThrows(BadRequestException.class,
                () -> dgmWardenService.createWarden(blankReq, "dgm.director@hostelhub.edu"));

        WardenCreateRequest tooLongReq = new WardenCreateRequest();
        tooLongReq.setEmployeeId("WAR-SVC-2");
        tooLongReq.setName("Service Warden");
        tooLongReq.setEmail("warden.svc2@hostelhub.edu");
        tooLongReq.setHostelId(hostel.getId());
        tooLongReq.setPassword("y".repeat(101));
        assertThrows(BadRequestException.class,
                () -> dgmWardenService.createWarden(tooLongReq, "dgm.director@hostelhub.edu"));

        assertFalse(userRepository.existsByEmail("warden.svc@hostelhub.edu"));
        assertFalse(userRepository.existsByEmail("warden.svc2@hostelhub.edu"));

        // Existing non-DGM authorization still returns 403 Forbidden
        String wardenToken = login("warden.vance@hostelhub.edu", "wardenPass2026");
        Map<String, Object> payload = basePayload("WAR-UNAUTH", "warden.unauth@hostelhub.edu", hostel.getId());
        payload.put("password", "ValidPass!2026");

        mockMvc.perform(post("/api/dgm/wardens")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    private Map<String, Object> basePayload(String employeeId, String email, Long hostelId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("employeeId", employeeId);
        payload.put("name", "New Test Warden");
        payload.put("email", email);
        payload.put("phone", "+91 98765 44444");
        payload.put("hostelId", hostelId);
        payload.put("assignedFloors", List.of(1, 2));
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

