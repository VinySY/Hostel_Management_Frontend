package com.hostelsync.backend.controller;

import com.hostelsync.backend.entity.NoticeCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies all backend NoticeCategory enum values work end-to-end through /api/notices.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.seeder.enabled=true")
class NoticeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void noticeEndpoints_supportAllSixBackendNoticeCategories() throws Exception {
        String wardenToken = login("warden.vance@hostelhub.edu", "wardenPass2026");
        String studentToken = login("rahul.sharma@hostelhub.edu", "studentPass2026");

        for (NoticeCategory category : NoticeCategory.values()) {
            String title = "Notice for " + category.name();
            mockMvc.perform(post("/api/notices")
                            .header("Authorization", "Bearer " + wardenToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", title,
                                    "message", "Verification message for category " + category.name(),
                                    "category", category.name(),
                                    "priority", category == NoticeCategory.EMERGENCY ? "URGENT" : "MEDIUM"
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.category", is(category.name())));
        }

        for (NoticeCategory category : NoticeCategory.values()) {
            mockMvc.perform(get("/api/notices")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].category", hasItem(category.name())));
        }
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

