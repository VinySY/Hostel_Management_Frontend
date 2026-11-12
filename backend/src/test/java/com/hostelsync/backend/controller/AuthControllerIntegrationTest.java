package com.hostelsync.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end authentication tests against the real SecurityConfig,
 * UserDetailsService, BCrypt encoder and DataSeeder (H2 test database).
 *
 * The seeder is enabled for this context so the three configured demo
 * accounts are exercised exactly as they are in development.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.seeder.enabled=true")
class AuthControllerIntegrationTest {

    private static final String STUDENT_EMAIL = "rahul.sharma@hostelhub.edu";
    private static final String STUDENT_PASSWORD = "studentPass2026";
    private static final String WARDEN_EMAIL = "warden.vance@hostelhub.edu";
    private static final String WARDEN_PASSWORD = "wardenPass2026";
    private static final String DGM_EMAIL = "dgm.director@hostelhub.edu";
    private static final String DGM_PASSWORD = "dgmPass2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void studentDemoLoginSucceeds() throws Exception {
        loginRequest(STUDENT_EMAIL, STUDENT_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is(STUDENT_EMAIL)))
                .andExpect(jsonPath("$.user.role", is("STUDENT")))
                .andExpect(jsonPath("$.user.studentId", is("STU-2023-089")));
    }

    @Test
    void wardenDemoLoginSucceeds() throws Exception {
        loginRequest(WARDEN_EMAIL, WARDEN_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is(WARDEN_EMAIL)))
                .andExpect(jsonPath("$.user.role", is("WARDEN")));
    }

    @Test
    void dgmDemoLoginSucceeds() throws Exception {
        loginRequest(DGM_EMAIL, DGM_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is(DGM_EMAIL)))
                .andExpect(jsonPath("$.user.role", is("DGM")));
    }

    @Test
    void emailWithSurroundingWhitespaceIsAccepted() throws Exception {
        loginRequest("  " + DGM_EMAIL + "  ", DGM_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("DGM")));
    }

    @Test
    void invalidPasswordIsRejectedWith401() throws Exception {
        loginRequest(DGM_EMAIL, "not-the-demo-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    void nonExistentAccountIsRejectedWith401() throws Exception {
        loginRequest("does.not.exist@hostelhub.edu", "whatever")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    void malformedLoginBodyIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jwtAllowsDgmDashboardAndStudentProfile() throws Exception {
        String dgmToken = tokenFor(DGM_EMAIL, DGM_PASSWORD);
        mockMvc.perform(get("/api/dgm/dashboard")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalComplaints", notNullValue()));

        String studentToken = tokenFor(STUDENT_EMAIL, STUDENT_PASSWORD);
        mockMvc.perform(get("/api/students/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(STUDENT_EMAIL)))
                .andExpect(jsonPath("$.studentId", is("STU-2023-089")));
    }

    @Test
    void jwtPreservesRoleBasedAuthorization() throws Exception {
        String studentToken = tokenFor(STUDENT_EMAIL, STUDENT_PASSWORD);
        mockMvc.perform(get("/api/dgm/dashboard")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        String wardenToken = tokenFor(WARDEN_EMAIL, WARDEN_PASSWORD);
        mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointIsPublicAndReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("HostelSync Backend")));
    }

    @Test
    void protectedEndpointRejectsMissingOrInvalidToken() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().is4xxClientError());
    }

    private org.springframework.test.web.servlet.ResultActions loginRequest(String email, String password)
            throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", password))));
    }

    private String tokenFor(String email, String password) throws Exception {
        MvcResult result = loginRequest(email, password)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(body.hasNonNull("token"), "Login response should contain a JWT");
        String token = body.get("token").asText();
        assertEquals(3, token.split("\\.").length, "Expected a three-part JWT");
        return token;
    }
}
