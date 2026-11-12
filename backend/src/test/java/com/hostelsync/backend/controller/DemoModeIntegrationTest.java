package com.hostelsync.backend.controller;

import com.hostelsync.backend.config.DemoAccountRegistry;
import com.hostelsync.backend.config.DemoSandboxService;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.job.OverdueDetectionJob;
import com.hostelsync.backend.job.SlaReminderJob;
import com.hostelsync.backend.repository.*;
import com.hostelsync.backend.security.JwtUtil;
import com.hostelsync.backend.service.AuditService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end verification of passwordless Demo Mode:
 * - Sandbox seeding & idempotency & non-guessable BCrypt hashes
 * - Passwordless login for STUDENT, WARDEN, DGM with demo=true claim and DTO flag
 * - Strict read-only enforcement (POST/PUT/PATCH/DELETE -> 403, zero DB mutations)
 * - Strict isolation of demo users from real institutional hostels, rooms, students,
 *   wardens, complaints, escalations, notices, surveys, tasks, files, and audit logs
 * - Scheduled job skipping for demo complaints & uninterrupted real user flows
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo_mode_it;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=YEAR",
        "app.demo.enabled=true",
        "app.demo.seed-enabled=true",
        "app.jobs.enabled=true"
})
class DemoModeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DemoAccountRegistry demoAccountRegistry;
    @Autowired private DemoSandboxService demoSandboxService;
    @Autowired private OverdueDetectionJob overdueDetectionJob;
    @Autowired private SlaReminderJob slaReminderJob;
    @Autowired private AuditService auditService;

    @Autowired private HostelRepository hostelRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private WardenRepository wardenRepository;
    @Autowired private DgmRepository dgmRepository;
    @Autowired private HostelDgmAssignmentRepository hostelDgmAssignmentRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private ComplaintAttachmentRepository attachmentRepository;
    @Autowired private ComplaintEscalationRepository escalationRepository;
    @Autowired private NoticeRepository noticeRepository;
    @Autowired private SurveyRepository surveyRepository;
    @Autowired private FeedbackTaskRepository feedbackTaskRepository;

    private Hostel realHostel;
    private Room realRoom;
    private User realStudentUser;
    private Student realStudent;
    private User realWardenUser;
    private Warden realWarden;
    private Complaint realComplaint;
    private ComplaintAttachment realAttachment;
    private ComplaintEscalation realEscalation;
    private Notice realHostelNotice;
    private Notice globalNotice;
    private Survey realSurvey;
    private Survey globalSurvey;
    private FeedbackTask realTask;

    @BeforeAll
    void seedRealInstitutionalDataAlongsideDemoSandbox() {
        realHostel = new Hostel("North Institutional Block (Real)", "Campus Road 1");
        realHostel.setActive(true);
        realHostel = hostelRepository.saveAndFlush(realHostel);

        realRoom = new Room("R-901", "Real Block", 9, 4, realHostel);
        realRoom.setActive(true);
        realRoom = roomRepository.saveAndFlush(realRoom);

        realStudentUser = userRepository.saveAndFlush(new User(
                "Private Real Student",
                "real.student@inst.edu",
                passwordEncoder.encode("RealStudentPass!2026"),
                UserRole.STUDENT
        ));
        realStudentUser.setAvatarUrl("/api/files/real-student-avatar.png");
        realStudentUser = userRepository.saveAndFlush(realStudentUser);

        realStudent = new Student();
        realStudent.setUser(realStudentUser);
        realStudent.setStudentId("REAL-STU-901");
        realStudent.setCourse("B.Tech Civil");
        realStudent.setYear(2);
        realStudent.setPhone("+91 98888 11111");
        realStudent.setGender("Male");
        realStudent.setGuardianName("Real Parent");
        realStudent.setGuardianPhone("+91 98888 22222");
        realStudent.setRoom(realRoom);
        realStudent.setStatus(StudentStatus.Active);
        realStudent = studentRepository.saveAndFlush(realStudent);

        realWardenUser = userRepository.saveAndFlush(new User(
                "Real Institutional Warden",
                "real.warden@inst.edu",
                passwordEncoder.encode("RealWardenPass!2026"),
                UserRole.WARDEN
        ));
        realWarden = new Warden();
        realWarden.setUser(realWardenUser);
        realWarden.setEmployeeId("REAL-WDN-901");
        realWarden.setHostel(realHostel);
        realWarden.setPhone("+91 98888 33333");
        realWarden.setActive(true);
        realWarden = wardenRepository.saveAndFlush(realWarden);

        User realDgmUser = userRepository.saveAndFlush(new User(
                "Real Institutional DGM",
                "real.dgm@inst.edu",
                passwordEncoder.encode("RealDgmPass!2026"),
                UserRole.DGM
        ));
        Dgm realDgm = new Dgm();
        realDgm.setUser(realDgmUser);
        realDgm.setEmployeeId("REAL-DGM-901");
        realDgm.setDesignation("Director");
        realDgm.setOfficeLocation("Main Admin");
        realDgm.setContactExtension("901");
        realDgm = dgmRepository.saveAndFlush(realDgm);

        HostelDgmAssignment dgmAssignment = new HostelDgmAssignment();
        dgmAssignment.setHostel(realHostel);
        dgmAssignment.setDgm(realDgm);
        dgmAssignment.setAssignedBy("SYSTEM");
        hostelDgmAssignmentRepository.saveAndFlush(dgmAssignment);

        realComplaint = new Complaint();
        realComplaint.setComplaintNumber("CMP-REAL-901");
        realComplaint.setStudent(realStudent);
        realComplaint.setType(ComplaintType.Electricity);
        realComplaint.setPriority(ComplaintPriority.Urgent);
        realComplaint.setSubject("CONFIDENTIAL REAL COMPLAINT");
        realComplaint.setDescription("Sensitive real hostel incident description.");
        realComplaint.setLocation("Real Room 901");
        realComplaint.setStatus(ComplaintStatus.In_Progress);
        realComplaint.setAssignedWarden(realWarden);
        realComplaint.setSlaDeadline(LocalDateTime.now().minusHours(1));
        realComplaint = complaintRepository.saveAndFlush(realComplaint);

        realAttachment = new ComplaintAttachment(
                realComplaint,
                "private-evidence.png",
                "real-private-evidence.png",
                "image/png",
                1024L
        );
        realAttachment = attachmentRepository.saveAndFlush(realAttachment);

        realEscalation = new ComplaintEscalation();
        realEscalation.setComplaint(realComplaint);
        realEscalation.setEscalatedByStudent(realStudent);
        realEscalation.setReason("Urgent institutional escalation");
        realEscalation.setSupportingDetails("Private details");
        realEscalation.setStatus(EscalationStatus.PENDING);
        realEscalation = escalationRepository.saveAndFlush(realEscalation);

        auditService.log("Complaint", realComplaint.getId(), "STATUS_CHANGE",
                realWardenUser, "Submitted", "In Progress", "Private institutional note", null);

        realHostelNotice = new Notice();
        realHostelNotice.setTitle("CONFIDENTIAL REAL HOSTEL NOTICE");
        realHostelNotice.setMessage("Only for North Institutional Block.");
        realHostelNotice.setCategory(NoticeCategory.SECURITY);
        realHostelNotice.setPriority(NoticePriority.URGENT);
        realHostelNotice.setHostel(realHostel);
        realHostelNotice.setAuthorName("Real Warden");
        realHostelNotice.setAuthorRole("WARDEN");
        realHostelNotice.setActive(true);
        realHostelNotice = noticeRepository.saveAndFlush(realHostelNotice);

        globalNotice = new Notice();
        globalNotice.setTitle("GLOBAL REAL CAMPUS NOTICE");
        globalNotice.setMessage("Real campus notice for institutional hostels.");
        globalNotice.setCategory(NoticeCategory.GENERAL);
        globalNotice.setPriority(NoticePriority.MEDIUM);
        globalNotice.setHostel(null);
        globalNotice.setAuthorName("Real DGM");
        globalNotice.setAuthorRole("DGM");
        globalNotice.setActive(true);
        globalNotice = noticeRepository.saveAndFlush(globalNotice);

        realSurvey = new Survey();
        realSurvey.setTitle("CONFIDENTIAL REAL HOSTEL SURVEY");
        realSurvey.setDescription("Only for real hostel residents.");
        realSurvey.setTargetHostel(realHostel);
        realSurvey.setCreatedBy("real.dgm@inst.edu");
        realSurvey.setActive(true);
        realSurvey = surveyRepository.saveAndFlush(realSurvey);

        globalSurvey = new Survey();
        globalSurvey.setTitle("GLOBAL REAL CAMPUS SURVEY");
        globalSurvey.setDescription("Real campus survey.");
        globalSurvey.setTargetHostel(null);
        globalSurvey.setCreatedBy("real.dgm@inst.edu");
        globalSurvey.setActive(true);
        globalSurvey = surveyRepository.saveAndFlush(globalSurvey);

        realTask = new FeedbackTask();
        realTask.setTitle("CONFIDENTIAL REAL TASK");
        realTask.setDescription("Real warden task");
        realTask.setHostel(realHostel);
        realTask.setWarden(realWarden);
        realTask.setSurvey(realSurvey);
        realTask.setStatus(FeedbackTaskStatus.PENDING);
        realTask.setCreatedBy("real.dgm@inst.edu");
        realTask = feedbackTaskRepository.saveAndFlush(realTask);
    }

    @Test
    void demoSandboxIsIdempotentAndUsesNonGuessableBcryptHashes() {
        long hostelCountBefore = hostelRepository.count();
        long userCountBefore = userRepository.count();
        long complaintCountBefore = complaintRepository.count();

        demoSandboxService.ensureDemoSandboxInitialized();

        assertEquals(hostelCountBefore, hostelRepository.count());
        assertEquals(userCountBefore, userRepository.count());
        assertEquals(complaintCountBefore, complaintRepository.count());

        User demoStudent = userRepository.findByEmailIgnoreCase("demo.student@demo.hostelsync.local").orElseThrow();
        User demoWarden = userRepository.findByEmailIgnoreCase("demo.warden@demo.hostelsync.local").orElseThrow();
        User demoDgm = userRepository.findByEmailIgnoreCase("demo.dgm@demo.hostelsync.local").orElseThrow();

        assertTrue(demoStudent.getPasswordHash().startsWith("$2"));
        assertTrue(demoWarden.getPasswordHash().startsWith("$2"));
        assertTrue(demoDgm.getPasswordHash().startsWith("$2"));
        assertFalse(passwordEncoder.matches("studentPass2026", demoStudent.getPasswordHash()));
        assertFalse(passwordEncoder.matches("wardenPass2026", demoWarden.getPasswordHash()));
        assertFalse(passwordEncoder.matches("dgmPass2026", demoDgm.getPasswordHash()));
    }

    @Test
    void passwordlessDemoLoginWorksForAllThreeRolesAndRejectsInvalidRoles() throws Exception {
        for (UserRole role : List.of(UserRole.STUDENT, UserRole.WARDEN, UserRole.DGM)) {
            MvcResult res = mockMvc.perform(post("/api/auth/demo-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("role", role.name()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.user.role", is(role.name())))
                    .andExpect(jsonPath("$.user.demo", is(true)))
                    .andReturn();

            JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
            String token = root.get("token").asText();
            assertTrue(jwtUtil.extractDemo(token));
            assertTrue(demoAccountRegistry.isDemoEmail(jwtUtil.extractEmail(token)));
        }

        mockMvc.perform(post("/api/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void demoUsersCannotMutateAnythingAcrossHttpWriteMethods() throws Exception {
        String studentToken = fetchDemoToken(UserRole.STUDENT);
        String wardenToken = fetchDemoToken(UserRole.WARDEN);
        String dgmToken = fetchDemoToken(UserRole.DGM);

        long complaintsBefore = complaintRepository.count();
        long noticesBefore = noticeRepository.count();
        long hostelsBefore = hostelRepository.count();
        long surveysBefore = surveyRepository.count();

        mockMvc.perform(post("/api/complaints/" + realComplaint.getId() + "/reopen")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("read-only")));

        mockMvc.perform(patch("/api/complaints/" + realComplaint.getId() + "/status")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Resolved\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("read-only")));

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hack\",\"message\":\"Hack\",\"category\":\"GENERAL\",\"priority\":\"MEDIUM\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("read-only")));

        mockMvc.perform(put("/api/students/" + realStudent.getId())
                        .header("Authorization", "Bearer " + wardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mutated\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/students/" + realStudent.getId())
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/hostels")
                        .header("Authorization", "Bearer " + dgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rogue Hostel\",\"address\":\"X\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("read-only")));

        mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + dgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Rogue\"}"))
                .andExpect(status().isForbidden());

        assertEquals(complaintsBefore, complaintRepository.count());
        assertEquals(noticesBefore, noticeRepository.count());
        assertEquals(hostelsBefore, hostelRepository.count());
        assertEquals(surveysBefore, surveyRepository.count());
    }

    @Test
    void demoUsersAreStrictlyIsolatedFromRealAndGlobalRecords() throws Exception {
        String studentToken = fetchDemoToken(UserRole.STUDENT);
        String wardenToken = fetchDemoToken(UserRole.WARDEN);
        String dgmToken = fetchDemoToken(UserRole.DGM);

        // List endpoints for Demo Warden must ONLY expose showcase hostel data
        String wardenStudents = mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(wardenStudents.contains("REAL-STU-901"));
        assertTrue(wardenStudents.contains("STU-DEMO-101"));

        String wardenRooms = mockMvc.perform(get("/api/rooms")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(wardenRooms.contains("R-901"));
        assertTrue(wardenRooms.contains("D-101"));

        String wardenComplaints = mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(wardenComplaints.contains("CONFIDENTIAL REAL COMPLAINT"));
        assertTrue(wardenComplaints.contains("Kulkarni") || wardenComplaints.contains("CMP-"));

        String dashboardStats = mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode statsJson = objectMapper.readTree(dashboardStats);
        assertEquals(4L, statsJson.get("totalStudents").asLong());
        assertEquals(4L, statsJson.get("totalRooms").asLong());

        // Notices exclude both real-hostel AND global (hostel=null) notices
        for (String token : List.of(studentToken, wardenToken, dgmToken)) {
            String noticesBody = mockMvc.perform(get("/api/notices")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertFalse(noticesBody.contains("CONFIDENTIAL REAL HOSTEL NOTICE"));
            assertFalse(noticesBody.contains("GLOBAL REAL CAMPUS NOTICE"));
        }

        // Surveys exclude both real-hostel AND global (targetHostel=null) surveys
        String studentSurveys = mockMvc.perform(get("/api/surveys/active")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(studentSurveys.contains("CONFIDENTIAL REAL HOSTEL SURVEY"));
        assertFalse(studentSurveys.contains("GLOBAL REAL CAMPUS SURVEY"));

        String dgmSurveys = mockMvc.perform(get("/api/surveys")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(dgmSurveys.contains("CONFIDENTIAL REAL HOSTEL SURVEY"));
        assertFalse(dgmSurveys.contains("GLOBAL REAL CAMPUS SURVEY"));

        // DGM endpoints exclude real hostels, real wardens, real escalations, and real feedback tasks
        mockMvc.perform(get("/api/hostels")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("HostelSync Showcase Hostel (Demo)")));

        mockMvc.perform(get("/api/dgm/hostels")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        String dgmWardens = mockMvc.perform(get("/api/dgm/wardens")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(dgmWardens.contains("REAL-WDN-901"));
        assertTrue(dgmWardens.contains("WDN-DEMO-01"));

        String pendingEscalations = mockMvc.perform(get("/api/dgm/escalations/pending")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(pendingEscalations.contains("CONFIDENTIAL REAL COMPLAINT"));

        String globalPendingEscalations = mockMvc.perform(get("/api/complaints/escalations/pending")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(globalPendingEscalations.contains("CONFIDENTIAL REAL COMPLAINT"));

        String dgmTasks = mockMvc.perform(get("/api/feedback-tasks")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(dgmTasks.contains("CONFIDENTIAL REAL TASK"));

        // Direct ID access to ANY real institutional resource -> 403 Forbidden
        mockMvc.perform(get("/api/students/" + realStudent.getId())
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/students/by-student-id/REAL-STU-901")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/rooms/" + realRoom.getId())
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/rooms/" + realRoom.getId() + "/furniture/history")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hostels/" + realHostel.getId())
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hostels/" + realHostel.getId() + "/details")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/complaints/" + realComplaint.getId())
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/complaints/" + realComplaint.getId() + "/history")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/complaints/" + realComplaint.getId() + "/escalations")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/complaints/escalations/" + realEscalation.getId())
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dgm/escalations/" + realComplaint.getId() + "/history")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dgm/wardens/" + realWarden.getId())
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/surveys/" + realSurvey.getId() + "/analytics")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/surveys/" + globalSurvey.getId() + "/analytics")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit-logs/entity/Complaint/" + realComplaint.getId())
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit-logs/complaint/" + realComplaint.getId())
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit-logs/search")
                        .param("entityType", "Complaint")
                        .param("entityId", String.valueOf(realComplaint.getId()))
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit-logs/user/" + realWardenUser.getId())
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/files/" + realAttachment.getStoredFileName())
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/files/real-student-avatar.png")
                        .header("Authorization", "Bearer " + dgmToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void scheduledJobsSkipDemoComplaintsWhileProcessingRealComplaints() {
        Hostel demoHostel = hostelRepository.findByName("HostelSync Showcase Hostel (Demo)").orElseThrow();
        List<Complaint> demoComplaints = complaintRepository.findByHostelId(demoHostel.getId());
        Complaint demoActive = demoComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.In_Progress)
                .findFirst()
                .orElseThrow();

        // Force both a demo complaint and the real complaint to be past SLA deadline
        demoActive.setSlaDeadline(LocalDateTime.now().minusMinutes(30));
        complaintRepository.saveAndFlush(demoActive);

        realComplaint.setSlaDeadline(LocalDateTime.now().minusMinutes(30));
        realComplaint.setStatus(ComplaintStatus.In_Progress);
        realComplaint.setOverdueAt(null);
        complaintRepository.saveAndFlush(realComplaint);

        overdueDetectionJob.detectOverdueComplaints();
        slaReminderJob.sendSlaReminders();

        Complaint refreshedDemo = complaintRepository.findById(demoActive.getId()).orElseThrow();
        Complaint refreshedReal = complaintRepository.findById(realComplaint.getId()).orElseThrow();

        assertEquals(ComplaintStatus.In_Progress, refreshedDemo.getStatus());
        assertNull(refreshedDemo.getOverdueAt());
        assertNull(refreshedDemo.getReminderSentAt());

        assertEquals(ComplaintStatus.Overdue, refreshedReal.getStatus());
        assertNotNull(refreshedReal.getOverdueAt());
    }

    @Test
    void realUsersRetainNormalAuthenticationAndWriteCapabilities() throws Exception {
        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "real.warden@inst.edu",
                                "password", "RealWardenPass!2026"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.demo", is(false)))
                .andReturn();

        String realToken = objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("token").asText();
        assertFalse(jwtUtil.extractDemo(realToken));

        mockMvc.perform(post("/api/notices")
                        .header("Authorization", "Bearer " + realToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Real Scheduled Drill\",\"message\":\"Drill tomorrow at 9 AM\",\"category\":\"GENERAL\",\"priority\":\"MEDIUM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Real Scheduled Drill")));
    }

    @Test
    void institutionalUsersAreStrictlyIsolatedFromDemoRecordsAndCrossEnvironmentWritesAreRejected() throws Exception {
        MvcResult wardenLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "real.warden@inst.edu",
                                "password", "RealWardenPass!2026"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String realWardenToken = objectMapper.readTree(wardenLoginRes.getResponse().getContentAsString()).get("token").asText();

        MvcResult dgmLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "real.dgm@inst.edu",
                                "password", "RealDgmPass!2026"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String realDgmToken = objectMapper.readTree(dgmLoginRes.getResponse().getContentAsString()).get("token").asText();

        Hostel demoHostel = hostelRepository.findByName("HostelSync Showcase Hostel (Demo)").orElseThrow();
        Student demoStudent = studentRepository.findByStudentId("STU-DEMO-101").orElseThrow();
        Room demoRoom = roomRepository.findByHostelId(demoHostel.getId()).get(0);
        Complaint demoComplaint = complaintRepository.findByHostelId(demoHostel.getId()).get(0);
        Warden demoWarden = wardenRepository.findByHostelId(demoHostel.getId()).get(0);

        // 1. Institutional warden list endpoints MUST NOT leak demo records
        String studentsJson = mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + realWardenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(studentsJson.contains("REAL-STU-901"));
        assertFalse(studentsJson.contains("STU-DEMO-101"));

        String roomsJson = mockMvc.perform(get("/api/rooms")
                        .header("Authorization", "Bearer " + realWardenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(roomsJson.contains("R-901"));
        assertFalse(roomsJson.contains("D-101"));

        String complaintsJson = mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + realWardenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(complaintsJson.contains("CMP-REAL-901"));
        assertFalse(complaintsJson.contains("CMP-DEMO-"));

        // 2. Institutional dashboard stats MUST NOT include demo counts
        String statsJson = mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + realWardenToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode stats = objectMapper.readTree(statsJson);
        assertEquals(1L, stats.get("totalStudents").asLong());
        assertEquals(1L, stats.get("totalRooms").asLong());

        // 3. Institutional DGM lists MUST NOT leak demo hostel or demo wardens
        String hostelsJson = mockMvc.perform(get("/api/hostels")
                        .header("Authorization", "Bearer " + realDgmToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(hostelsJson.contains("North Institutional Block (Real)"));
        assertFalse(hostelsJson.contains("HostelSync Showcase Hostel (Demo)"));

        String wardensJson = mockMvc.perform(get("/api/dgm/wardens")
                        .header("Authorization", "Bearer " + realDgmToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(wardensJson.contains("REAL-WDN-901"));
        assertFalse(wardensJson.contains("WDN-DEMO-01"));

        // 4. Institutional direct GET access to demo records MUST be rejected with 403 Forbidden
        mockMvc.perform(get("/api/students/" + demoStudent.getId())
                        .header("Authorization", "Bearer " + realWardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/rooms/" + demoRoom.getId())
                        .header("Authorization", "Bearer " + realWardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/complaints/" + demoComplaint.getId())
                        .header("Authorization", "Bearer " + realWardenToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hostels/" + demoHostel.getId())
                        .header("Authorization", "Bearer " + realDgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hostels/" + demoHostel.getId() + "/details")
                        .header("Authorization", "Bearer " + realDgmToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dgm/wardens/" + demoWarden.getId())
                        .header("Authorization", "Bearer " + realDgmToken))
                .andExpect(status().isForbidden());

        // 5. Cross-environment write attempts MUST be rejected with 403 Forbidden
        // Attempting to create an institutional room inside the demo hostel
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + realDgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomNumber", "R-ROGUE-1",
                                "block", "Block A",
                                "floor", 1,
                                "capacity", 2,
                                "hostelId", demoHostel.getId()
                        ))))
                .andExpect(status().isForbidden());

        // Attempting to register an institutional student inside the demo hostel
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + realWardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Rogue Student",
                                "email", "rogue.student@inst.edu",
                                "password", "RoguePass!2026",
                                "studentId", "ROGUE-STU-01",
                                "course", "B.Tech",
                                "year", 1,
                                "phone", "+91 99999 00001",
                                "roomId", demoRoom.getId()
                        ))))
                .andExpect(status().isForbidden());

        // Attempting to create an institutional hostel with the reserved demo name
        mockMvc.perform(post("/api/hostels")
                        .header("Authorization", "Bearer " + realDgmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "HostelSync Showcase Hostel (Demo)",
                                "address", "Fake Campus"
                        ))))
                .andExpect(status().isForbidden());

        // Attempting to register an institutional student with a reserved demo email domain
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + realWardenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Spoofed Demo Student",
                                "email", "new.spoof@demo.hostelsync.local",
                                "password", "SpoofPass!2026",
                                "studentId", "SPOOF-STU-01",
                                "course", "B.Tech",
                                "year", 1,
                                "phone", "+91 99999 00002",
                                "roomId", realRoom.getId()
                        ))))
                .andExpect(status().isForbidden());
    }

    private String fetchDemoToken(UserRole role) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", role.name()))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }
}
