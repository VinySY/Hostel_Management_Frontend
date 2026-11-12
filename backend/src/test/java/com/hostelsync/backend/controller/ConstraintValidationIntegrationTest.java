package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.RoomCreateRequest;
import com.hostelsync.backend.dto.RoomUpdateRequest;
import com.hostelsync.backend.dto.StudentUpdateRequest;
import com.hostelsync.backend.dto.SurveySubmissionRequest;
import com.hostelsync.backend.entity.ComplaintPriority;
import com.hostelsync.backend.entity.Survey;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.repository.HostelRepository;
import com.hostelsync.backend.repository.RoomRepository;
import com.hostelsync.backend.repository.StudentRepository;
import com.hostelsync.backend.repository.SurveyRepository;
import com.hostelsync.backend.service.RoomService;
import com.hostelsync.backend.service.SlaService;
import com.hostelsync.backend.service.StudentService;
import com.hostelsync.backend.service.SurveyService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that all 15 original MySQL CHECK constraint rules are enforced at the
 * Spring Boot API (@Valid DTO) and service layers for TiDB Cloud Starter compatibility.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.seeder.enabled=true")
class ConstraintValidationIntegrationTest {

    private static final String WARDEN_EMAIL = "warden.vance@hostelhub.edu";
    private static final String WARDEN_PASSWORD = "wardenPass2026";
    private static final String STUDENT_EMAIL = "rahul.sharma@hostelhub.edu";
    private static final String STUDENT_PASSWORD = "studentPass2026";
    private static final String DGM_EMAIL = "dgm.director@hostelhub.edu";
    private static final String DGM_PASSWORD = "dgmPass2026";

    private static final List<String> SURVEY_RATING_FIELDS = List.of(
            "q1Cleanliness",
            "q2Washrooms",
            "q3Furniture",
            "q4DrinkingWater",
            "q5Wifi",
            "q6Electricity",
            "q7Food",
            "q8Security",
            "q9WardenSupport",
            "q10ComplaintResolution"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private SlaService slaService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SurveyService surveyService;

    @Test
    void roomConstraints_rejectInvalidAndAcceptValidBoundaries() throws Exception {
        String wardenToken = login(WARDEN_EMAIL, WARDEN_PASSWORD);
        Long hostelId = hostelRepository.findAll().get(0).getId();

        // Invalid floor (-1) and capacities (0, 21) on POST /api/rooms
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomNumber", "CHK-BAD-1",
                                "block", "Block C",
                                "floor", -1,
                                "capacity", 2,
                                "hostelId", hostelId
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Floor must be 0 or greater")));

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomNumber", "CHK-BAD-2",
                                "block", "Block C",
                                "floor", 0,
                                "capacity", 0,
                                "hostelId", hostelId
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Capacity")));

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomNumber", "CHK-BAD-3",
                                "block", "Block C",
                                "floor", 0,
                                "capacity", 21,
                                "hostelId", hostelId
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Capacity")));

        // Valid minimum boundaries (floor=0, capacity=1)
        MvcResult created = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomNumber", "CHK-MIN-1",
                                "block", "Block C",
                                "floor", 0,
                                "capacity", 1,
                                "hostelId", hostelId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.floor", is(0)))
                .andExpect(jsonPath("$.capacity", is(1)))
                .andReturn();

        long roomId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Invalid updates on PUT /api/rooms/{id}
        mockMvc.perform(put("/api/rooms/" + roomId)
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("floor", -1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Floor must be 0 or greater")));

        mockMvc.perform(put("/api/rooms/" + roomId)
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("capacity", 21))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Capacity must be between 1 and 20")));

        // Valid maximum capacity boundary (capacity=20, floor=0)
        mockMvc.perform(put("/api/rooms/" + roomId)
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("floor", 0, "capacity", 20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floor", is(0)))
                .andExpect(jsonPath("$.capacity", is(20)));
    }

    @Test
    void slaConfigurationConstraints_rejectInvalidAndAcceptValidBoundaries() throws Exception {
        String wardenToken = login(WARDEN_EMAIL, WARDEN_PASSWORD);

        // Negative and zero API inputs rejected with HTTP 400
        mockMvc.perform(put("/api/sla-configurations/Urgent")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "priority", "Urgent",
                                "deadlineHours", -1,
                                "reminderThresholdPercent", 50
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Deadline hours")));

        mockMvc.perform(put("/api/sla-configurations/Urgent")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "priority", "Urgent",
                                "deadlineHours", 8,
                                "reminderThresholdPercent", -1
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Reminder threshold")));

        // Valid lower & upper API boundaries accepted
        mockMvc.perform(put("/api/sla-configurations/Urgent")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "priority", "Urgent",
                                "deadlineHours", 1,
                                "reminderThresholdPercent", 1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlineHours", is(1)))
                .andExpect(jsonPath("$.reminderThresholdPercent", is(1)));

        mockMvc.perform(put("/api/sla-configurations/Urgent")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "priority", "Urgent",
                                "deadlineHours", 720,
                                "reminderThresholdPercent", 99
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlineHours", is(720)))
                .andExpect(jsonPath("$.reminderThresholdPercent", is(99)));
    }

    @Test
    void studentYearConstraint_enforcedOnCreateAndUpdateIncludingBoundaries() throws Exception {
        String wardenToken = login(WARDEN_EMAIL, WARDEN_PASSWORD);

        // Invalid years on POST /api/students
        for (int badYear : List.of(0, 7, -1)) {
            mockMvc.perform(post("/api/students")
                            .header("Authorization", "Bearer " + wardenToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "name", "Test Student",
                                    "studentId", "STU-CHK-BAD-" + badYear,
                                    "email", "chk.bad." + badYear + "@hostelhub.edu",
                                    "password", "BoundaryPass#2026",
                                    "phone", "9876543210",
                                    "course", "B.Tech CS",
                                    "year", badYear
                            ))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Academic year must be between 1 and 6")));
        }

        // Valid minimum boundary (year=1) on POST /api/students
        MvcResult created = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Boundary Student",
                                "studentId", "STU-CHK-VALID-1",
                                "email", "chk.valid.1@hostelhub.edu",
                                "password", "BoundaryPass#2026",
                                "phone", "9876543210",
                                "course", "B.Tech CS",
                                "year", 1
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.year", is(1)))
                .andReturn();

        long studentDbId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Invalid years on PUT /api/students/{id} (StudentUpdateRequest)
        for (int badYear : List.of(0, 7, -2)) {
            mockMvc.perform(put("/api/students/" + studentDbId)
                            .header("Authorization", "Bearer " + wardenToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("year", badYear))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Academic year must be between 1 and 6")));
        }

        // Valid maximum boundary (year=6) on PUT /api/students/{id}
        mockMvc.perform(put("/api/students/" + studentDbId)
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("year", 6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year", is(6)));
    }

    @Test
    void surveySubmissionConstraints_enforcedForAll10QuestionsAndBoundaries() throws Exception {
        String dgmToken = login(DGM_EMAIL, DGM_PASSWORD);
        String studentToken = login(STUDENT_EMAIL, STUDENT_PASSWORD);

        long surveyMinId = createSurvey(dgmToken, "Boundary Min Survey");
        long surveyMaxId = createSurvey(dgmToken, "Boundary Max Survey");

        // All 10 rating questions must reject 0 and 6 with HTTP 400
        for (String field : SURVEY_RATING_FIELDS) {
            for (int invalidRating : List.of(0, 6)) {
                Map<String, Object> payload = validSurveyPayload(3);
                payload.put(field, invalidRating);

                mockMvc.perform(post("/api/surveys/" + surveyMinId + "/submit")
                                .header("Authorization", "Bearer " + studentToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message", containsString("Rating must be between 1 and 5")));
            }
        }

        // All 10 questions at valid minimum boundary (1) succeed
        mockMvc.perform(post("/api/surveys/" + surveyMinId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSurveyPayload(1))))
                .andExpect(status().isOk());

        // All 10 questions at valid maximum boundary (5) succeed
        mockMvc.perform(post("/api/surveys/" + surveyMaxId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSurveyPayload(5))))
                .andExpect(status().isOk());
    }

    @Test
    void serviceLayerValidation_protectsInternalCallsBypassingMvcValidation() {
        Long hostelId = hostelRepository.findAll().get(0).getId();
        Long existingRoomId = roomRepository.findAll().get(0).getId();
        Long existingStudentId = studentRepository.findAll().get(0).getId();
        Survey savedSurvey = surveyRepository.save(new Survey("SVC-SURVEY", "Service boundary test", null, "DGM"));

        // RoomService internal checks
        RoomCreateRequest badFloorRoom = new RoomCreateRequest();
        badFloorRoom.setRoomNumber("SVC-BAD-FLOOR");
        badFloorRoom.setBlock("Block A");
        badFloorRoom.setFloor(-1);
        badFloorRoom.setCapacity(2);
        badFloorRoom.setHostelId(hostelId);
        assertThrows(BadRequestException.class, () -> roomService.createRoom(badFloorRoom, "TEST"));

        RoomCreateRequest badCapacityRoom = new RoomCreateRequest();
        badCapacityRoom.setRoomNumber("SVC-BAD-CAP");
        badCapacityRoom.setBlock("Block A");
        badCapacityRoom.setFloor(1);
        badCapacityRoom.setCapacity(25);
        badCapacityRoom.setHostelId(hostelId);
        assertThrows(BadRequestException.class, () -> roomService.createRoom(badCapacityRoom, "TEST"));

        RoomUpdateRequest badRoomUpdate = new RoomUpdateRequest();
        badRoomUpdate.setCapacity(0);
        assertThrows(BadRequestException.class, () -> roomService.updateRoom(existingRoomId, badRoomUpdate, "TEST"));

        // SlaService internal checks
        assertThrows(BadRequestException.class, () -> slaService.upsertConfiguration(ComplaintPriority.High, -1, 50));
        assertThrows(BadRequestException.class, () -> slaService.upsertConfiguration(ComplaintPriority.High, 8, -1));

        // StudentService internal checks
        StudentUpdateRequest badStudentUpdate = new StudentUpdateRequest();
        badStudentUpdate.setYear(7);
        assertThrows(BadRequestException.class, () ->
                studentService.updateStudent(existingStudentId, badStudentUpdate, true, WARDEN_EMAIL));

        // SurveyService internal checks
        SurveySubmissionRequest badSurveyReq = new SurveySubmissionRequest();
        badSurveyReq.setQ10ComplaintResolution(6);
        assertThrows(BadRequestException.class, () ->
                surveyService.submitSurvey(savedSurvey.getId(), badSurveyReq, STUDENT_EMAIL));
    }

    private long createSurvey(String dgmToken, String title) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + dgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "description", "Validation boundary test survey"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private Map<String, Object> validSurveyPayload(int rating) {
        Map<String, Object> map = new HashMap<>();
        for (String field : SURVEY_RATING_FIELDS) {
            map.put(field, rating);
        }
        map.put("comments", "Boundary test comment");
        return map;
    }

    private String login(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.get("token").asText();
    }
}
