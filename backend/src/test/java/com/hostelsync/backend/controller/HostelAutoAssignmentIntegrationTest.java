package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.HostelCreateRequest;
import com.hostelsync.backend.dto.HostelResponse;
import com.hostelsync.backend.entity.Dgm;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.repository.HostelDgmAssignmentRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.DgmService;
import com.hostelsync.backend.service.HostelService;
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

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that creating a hostel as an authenticated DGM automatically assigns
 * ownership via HostelDgmAssignment, prevents other DGMs from seeing it,
 * avoids duplicate assignments, and leaves non-DGM/service creation unchanged.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:hostel_auto_assign_test;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=YEAR",
        "app.seeder.enabled=true",
        "app.bootstrap.enabled=false"
})
class HostelAutoAssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HostelDgmAssignmentRepository hostelDgmAssignmentRepository;

    @Autowired
    private DgmService dgmService;

    @Autowired
    private HostelService hostelService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createHostelAsDgm_autoAssignsCreatorOnlyAndPreservesOtherBehavior() throws Exception {
        // Seeded primary DGM: dgm.director@hostelhub.edu
        User primaryDgmUser = userRepository.findByEmail("dgm.director@hostelhub.edu").orElseThrow();
        Dgm primaryDgm = dgmService.getDgmByUserId(primaryDgmUser.getId());

        // Create a second independent DGM
        String secondDgmEmail = "dgm.second@hostelhub.edu";
        String secondDgmPassword = "SecondDgm!Pass#2026";
        User secondDgmUser = userRepository.findByEmail(secondDgmEmail)
                .orElseGet(() -> userRepository.save(new User(
                        "Second DGM",
                        secondDgmEmail,
                        passwordEncoder.encode(secondDgmPassword),
                        UserRole.DGM
                )));
        Dgm secondDgm = dgmService.createDgm(secondDgmUser, "DGM-002", "Associate Director", "Admin Block", "102");

        String secondDgmToken = login(secondDgmEmail, secondDgmPassword);
        String primaryDgmToken = login("dgm.director@hostelhub.edu", "dgmPass2026");

        // Second DGM creates a new hostel via POST /api/hostels
        String createdBody = mockMvc.perform(post("/api/hostels")
                        .header("Authorization", "Bearer " + secondDgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "East Wing Residence",
                                "address", "East Campus Road"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("East Wing Residence"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long createdHostelId = objectMapper.readTree(createdBody).get("id").asLong();

        // Creating DGM is automatically assigned and sees it immediately
        assertTrue(dgmService.hasAccessToHostel(secondDgm.getId(), createdHostelId));
        assertTrue(hostelDgmAssignmentRepository.findByHostelIdAndDgmId(createdHostelId, secondDgm.getId()).isPresent());

        mockMvc.perform(get("/api/dgm/hostels")
                        .header("Authorization", "Bearer " + secondDgmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("East Wing Residence")));

        mockMvc.perform(get("/api/dgm/dashboard")
                        .header("Authorization", "Bearer " + secondDgmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostelStats[*].hostelName", hasItem("East Wing Residence")));

        // Another DGM cannot automatically see that hostel
        assertFalse(dgmService.hasAccessToHostel(primaryDgm.getId(), createdHostelId));

        mockMvc.perform(get("/api/dgm/hostels")
                        .header("Authorization", "Bearer " + primaryDgmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem("East Wing Residence"))));

        mockMvc.perform(get("/api/dgm/dashboard")
                        .header("Authorization", "Bearer " + primaryDgmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostelStats[*].hostelName", not(hasItem("East Wing Residence"))));

        // Duplicate assignment protection remains intact
        assertThrows(BadRequestException.class,
                () -> dgmService.assignDgmToHostel(secondDgm.getId(), createdHostelId, secondDgmEmail));
        assertEquals(1, hostelDgmAssignmentRepository.findByHostelId(createdHostelId).size());

        // Existing explicit DGM assignment still works
        dgmService.assignDgmToHostel(primaryDgm.getId(), createdHostelId, secondDgmEmail);
        assertTrue(dgmService.hasAccessToHostel(primaryDgm.getId(), createdHostelId));

        // Service-level creation for non-DGM creator does not create a HostelDgmAssignment
        HostelCreateRequest nonDgmReq = new HostelCreateRequest();
        nonDgmReq.setName("Service Created Hostel");
        nonDgmReq.setAddress("South Campus");
        HostelResponse nonDgmCreated = hostelService.createHostel(nonDgmReq, "warden.vance@hostelhub.edu");
        assertTrue(hostelDgmAssignmentRepository.findByHostelId(nonDgmCreated.getId()).isEmpty());
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

