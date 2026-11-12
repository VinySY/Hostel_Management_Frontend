package com.hostelsync.backend.controller;

import com.hostelsync.backend.entity.Student;
import com.hostelsync.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authenticated file upload, relative file URL generation,
 * attachment ownership enforcement, and student photo (avatar) access control.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.seeder.enabled=true")
class FileControllerIntegrationTest {

    @TempDir
    static Path uploadTempDir;

    @DynamicPropertySource
    static void overrideUploadDir(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", () -> uploadTempDir.toAbsolutePath().toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void complaintAttachmentUploadUsesRelativeApiPathAndEnforcesOwnership() throws Exception {
        String ownerToken = loginToken("rahul.sharma@hostelhub.edu", "studentPass2026");
        String otherStudentToken = loginToken("aman.patel@hostelhub.edu", "studentPass2026");
        String wardenToken = loginToken("warden.vance@hostelhub.edu", "wardenPass2026");
        String dgmToken = loginToken("dgm.director@hostelhub.edu", "dgmPass2026");

        byte[] imageBytes = "fake-png-content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile complaintPart = new MockMultipartFile(
                "complaint",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(Map.of(
                        "type", "Electricity",
                        "subject", "Broken hallway light",
                        "description", "The corridor light flickers continuously at night.",
                        "location", "Block A, Room 204"
                ))
        );
        MockMultipartFile filePart = new MockMultipartFile(
                "files", "evidence.png", "image/png", imageBytes);

        MvcResult createResult = mockMvc.perform(multipart("/api/complaints")
                        .file(complaintPart)
                        .file(filePart)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachments[0].url", startsWith("/api/files/")))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String attachmentUrl = created.get("attachments").get(0).get("url").asText();
        assertFalse(attachmentUrl.contains("localhost"), "File URL must not hardcode localhost");

        // Unauthenticated download must be rejected
        mockMvc.perform(get(attachmentUrl))
                .andExpect(status().is4xxClientError());

        // Owner student can download
        mockMvc.perform(get(attachmentUrl)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(imageBytes));

        // Other student is forbidden
        mockMvc.perform(get(attachmentUrl)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());

        // Warden and DGM can access
        mockMvc.perform(get(attachmentUrl)
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(imageBytes));

        mockMvc.perform(get(attachmentUrl)
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    void studentAvatarUploadAndAuthorizedRetrievalWorks() throws Exception {
        String wardenToken = loginToken("warden.vance@hostelhub.edu", "wardenPass2026");
        String ownerToken = loginToken("rahul.sharma@hostelhub.edu", "studentPass2026");
        String otherStudentToken = loginToken("aman.patel@hostelhub.edu", "studentPass2026");

        Student rahul = studentRepository.findByStudentId("STU-2023-089").orElseThrow();
        byte[] avatarBytes = "fake-webp-avatar".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile photoPart = new MockMultipartFile(
                "file", "avatar.webp", "image/webp", avatarBytes);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/students/" + rahul.getId() + "/photo")
                        .file(photoPart)
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl", startsWith("/api/files/")))
                .andReturn();

        String avatarUrl = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("avatarUrl").asText();

        // Owner student and warden can fetch the avatar
        mockMvc.perform(get(avatarUrl).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(avatarBytes));

        mockMvc.perform(get(avatarUrl).header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(avatarBytes));

        // Another student cannot fetch someone else's private avatar file
        mockMvc.perform(get(avatarUrl).header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());

        // Non-existent file returns 404 for staff
        mockMvc.perform(get("/api/files/does-not-exist.webp")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isNotFound());
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }
}

