package com.hostelsync.backend.controller;

import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.repository.HostelRepository;
import com.hostelsync.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies Demo Mode stays completely disabled by default (ENABLE_PUBLIC_DEMO=false,
 * ENABLE_DEMO_SANDBOX_SEED=false).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo_mode_disabled_it;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=YEAR",
        "app.demo.enabled=false",
        "app.demo.seed-enabled=false"
})
class DemoModeDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void demoSandboxDoesNotSeedWhenFlagDisabled() {
        assertTrue(hostelRepository.findByName("HostelSync Showcase Hostel (Demo)").isEmpty());
        assertTrue(userRepository.findByEmailIgnoreCase("demo.student@demo.hostelsync.local").isEmpty());
        assertTrue(userRepository.findByEmailIgnoreCase("demo.warden@demo.hostelsync.local").isEmpty());
        assertTrue(userRepository.findByEmailIgnoreCase("demo.dgm@demo.hostelsync.local").isEmpty());
    }

    @Test
    void demoLoginIsRejectedForAllRolesWhenDisabled() throws Exception {
        for (String role : new String[]{"STUDENT", "WARDEN", "DGM"}) {
            mockMvc.perform(post("/api/auth/demo-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("role", role))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("disabled")));
        }
    }

    @Test
    void standardLoginAlsoBlocksDemoEmailWhenPublicDemoDisabled() throws Exception {
        User tempUser = userRepository.saveAndFlush(
                new User("Demo Student", "demo.student@demo.hostelsync.local",
                        passwordEncoder.encode("SecretPass123!"), UserRole.STUDENT));
        try {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", "demo.student@demo.hostelsync.local",
                                    "password", "SecretPass123!"
                            ))))
                    .andExpect(status().isForbidden());
        } finally {
            userRepository.delete(tempUser);
        }
    }
}
