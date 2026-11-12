package com.hostelsync.backend.config;

import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Idempotently provisions an isolated Showcase Demo Hostel and fictional demo accounts
 * (STUDENT, WARDEN, DGM) with random BCrypt hashes. Never modifies real institutional data.
 */
@Service
public class DemoSandboxService {

    private static final Logger log = LoggerFactory.getLogger(DemoSandboxService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DemoProperties demoProperties;
    private final HostelRepository hostelRepository;
    private final RoomRepository roomRepository;
    private final RoomFurnitureRepository roomFurnitureRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final WardenRepository wardenRepository;
    private final WardenFloorAssignmentRepository wardenFloorAssignmentRepository;
    private final DgmRepository dgmRepository;
    private final HostelDgmAssignmentRepository hostelDgmAssignmentRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintEscalationRepository escalationRepository;
    private final NoticeRepository noticeRepository;
    private final SurveyRepository surveyRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final FeedbackTaskRepository feedbackTaskRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoSandboxService(DemoProperties demoProperties,
                              HostelRepository hostelRepository,
                              RoomRepository roomRepository,
                              RoomFurnitureRepository roomFurnitureRepository,
                              UserRepository userRepository,
                              StudentRepository studentRepository,
                              WardenRepository wardenRepository,
                              WardenFloorAssignmentRepository wardenFloorAssignmentRepository,
                              DgmRepository dgmRepository,
                              HostelDgmAssignmentRepository hostelDgmAssignmentRepository,
                              ComplaintRepository complaintRepository,
                              ComplaintEscalationRepository escalationRepository,
                              NoticeRepository noticeRepository,
                              SurveyRepository surveyRepository,
                              SurveySubmissionRepository surveySubmissionRepository,
                              FeedbackTaskRepository feedbackTaskRepository,
                              PasswordEncoder passwordEncoder) {
        this.demoProperties = demoProperties;
        this.hostelRepository = hostelRepository;
        this.roomRepository = roomRepository;
        this.roomFurnitureRepository = roomFurnitureRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.wardenRepository = wardenRepository;
        this.wardenFloorAssignmentRepository = wardenFloorAssignmentRepository;
        this.dgmRepository = dgmRepository;
        this.hostelDgmAssignmentRepository = hostelDgmAssignmentRepository;
        this.complaintRepository = complaintRepository;
        this.escalationRepository = escalationRepository;
        this.noticeRepository = noticeRepository;
        this.surveyRepository = surveyRepository;
        this.surveySubmissionRepository = surveySubmissionRepository;
        this.feedbackTaskRepository = feedbackTaskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void ensureDemoSandboxInitialized() {
        Hostel demoHostel = hostelRepository.findByName(demoProperties.getHostelName().trim())
                .orElseGet(() -> {
                    Hostel created = new Hostel(
                            demoProperties.getHostelName().trim(),
                            demoProperties.getHostelAddress() != null
                                    ? demoProperties.getHostelAddress().trim()
                                    : "Showcase Demo Wing, University Campus"
                    );
                    created.setActive(true);
                    return hostelRepository.saveAndFlush(created);
                });

        Room d101 = ensureRoom(demoHostel, "D-101", "North Wing", 1, 3,
                CoolingEquipmentType.COOLER, CoolingMaintenanceStatus.OPERATIONAL, 0, 0);
        Room d102 = ensureRoom(demoHostel, "D-102", "North Wing", 1, 2,
                CoolingEquipmentType.AC, CoolingMaintenanceStatus.OPERATIONAL, 0, 0);
        Room d201 = ensureRoom(demoHostel, "D-201", "South Wing", 2, 3,
                CoolingEquipmentType.NONE, CoolingMaintenanceStatus.OPERATIONAL, 1, 0);
        ensureRoom(demoHostel, "D-202", "South Wing", 2, 2,
                CoolingEquipmentType.AC, CoolingMaintenanceStatus.MAINTENANCE_REQUIRED, 0, 1);

        User studentUser = ensureDemoUser(
                "Aarav Kulkarni (Demo)", demoProperties.getStudentEmail(), UserRole.STUDENT);
        Student demoStudent = ensureDemoStudent(
                studentUser, "STU-DEMO-101", "B.Tech Computer Science", 3, "+91 90000 10101",
                "Male", "Suresh Kulkarni", "+91 90000 10102", d101);

        User roommateUser2 = ensureDemoUser(
                "Meera Nair (Demo)", "demo.resident2@demo.hostelsync.local", UserRole.STUDENT);
        Student demoStudent2 = ensureDemoStudent(
                roommateUser2, "STU-DEMO-102", "B.Tech Electronics", 2, "+91 90000 10201",
                "Female", "Lakshmi Nair", "+91 90000 10202", d102);

        User roommateUser3 = ensureDemoUser(
                "Kabir Verma (Demo)", "demo.resident3@demo.hostelsync.local", UserRole.STUDENT);
        Student demoStudent3 = ensureDemoStudent(
                roommateUser3, "STU-DEMO-103", "B.Arch", 4, "+91 90000 10301",
                "Male", "Rajiv Verma", "+91 90000 10302", d101);

        User roommateUser4 = ensureDemoUser(
                "Zoya Khan (Demo)", "demo.resident4@demo.hostelsync.local", UserRole.STUDENT);
        ensureDemoStudent(
                roommateUser4, "STU-DEMO-104", "M.Tech Data Science", 1, "+91 90000 10401",
                "Female", "Imran Khan", "+91 90000 10402", d201);

        User wardenUser = ensureDemoUser(
                "Prof. Vikram Rao (Demo)", demoProperties.getWardenEmail(), UserRole.WARDEN);
        Warden demoWarden = ensureDemoWarden(wardenUser, demoHostel, "WDN-DEMO-01", "+91 90000 20001");

        User dgmUser = ensureDemoUser(
                "Dr. Ananya Sen (Demo)", demoProperties.getDgmEmail(), UserRole.DGM);
        Dgm demoDgm = ensureDemoDgm(dgmUser, demoHostel, "DGM-DEMO-01", "Director General (Demo Showcase)");

        seedSampleRecordsIfMissing(demoHostel, demoStudent, demoStudent2, demoStudent3, demoWarden, demoDgm);

        log.info("Demo sandbox ready: hostelId={}, demoStudentUserId={}, demoWardenUserId={}, demoDgmUserId={}.",
                demoHostel.getId(), studentUser.getId(), wardenUser.getId(), dgmUser.getId());
    }

    private User ensureDemoUser(String name, String rawEmail, UserRole role) {
        String email = rawEmail.trim().toLowerCase();
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            byte[] randomBytes = new byte[32];
            SECURE_RANDOM.nextBytes(randomBytes);
            String unguessableSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            User created = new User(name, email, passwordEncoder.encode(unguessableSecret), role);
            return userRepository.saveAndFlush(created);
        });
    }

    private Room ensureRoom(Hostel hostel, String number, String block, int floor, int capacity,
                            CoolingEquipmentType cooling, CoolingMaintenanceStatus maintenance,
                            int damagedCount, int underRepairCount) {
        return roomRepository.findByRoomNumberAndHostel(number, hostel).orElseGet(() -> {
            Room room = new Room(number, block, floor, capacity, hostel);
            room.setCoolingEquipment(cooling);
            room.setCoolingMaintenanceStatus(maintenance);
            room.setActive(true);
            Room saved = roomRepository.saveAndFlush(room);
            for (FurnitureType type : FurnitureType.values()) {
                RoomFurniture rf = new RoomFurniture(saved, type, capacity);
                int damaged = (type == FurnitureType.STUDY_TABLE) ? damagedCount : 0;
                int repair = (type == FurnitureType.LOCKER) ? underRepairCount : 0;
                rf.setServiceableCount(Math.max(0, capacity - damaged - repair));
                rf.setDamagedCount(damaged);
                rf.setMissingCount(0);
                rf.setUnderRepairCount(repair);
                roomFurnitureRepository.save(rf);
            }
            return saved;
        });
    }

    private Student ensureDemoStudent(User user, String studentId, String course, int year, String phone,
                                      String gender, String guardianName, String guardianPhone, Room room) {
        return studentRepository.findByUser(user).orElseGet(() -> {
            Student s = new Student();
            s.setUser(user);
            s.setStudentId(studentId);
            s.setCourse(course);
            s.setYear(year);
            s.setPhone(phone);
            s.setGender(gender);
            s.setGuardianName(guardianName);
            s.setGuardianPhone(guardianPhone);
            s.setStatus(StudentStatus.Active);
            s.setRoom(room);
            return studentRepository.saveAndFlush(s);
        });
    }

    private Warden ensureDemoWarden(User wardenUser, Hostel demoHostel, String employeeId, String phone) {
        Warden warden = wardenRepository.findByUser(wardenUser).orElseGet(() -> {
            Warden created = new Warden(wardenUser, employeeId, demoHostel, phone);
            created.setActive(true);
            return wardenRepository.saveAndFlush(created);
        });
        List<WardenFloorAssignment> existingFloors = wardenFloorAssignmentRepository.findByWardenId(warden.getId());
        if (existingFloors.isEmpty()) {
            wardenFloorAssignmentRepository.save(new WardenFloorAssignment(warden, demoHostel, 1));
            wardenFloorAssignmentRepository.save(new WardenFloorAssignment(warden, demoHostel, 2));
        }
        return warden;
    }

    private Dgm ensureDemoDgm(User dgmUser, Hostel demoHostel, String employeeId, String designation) {
        Dgm dgm = dgmRepository.findByUserId(dgmUser.getId()).orElseGet(() -> {
            Dgm created = new Dgm(dgmUser, employeeId, designation);
            created.setOfficeLocation("Demo Showcase Administration");
            created.setContactExtension("x9001");
            return dgmRepository.saveAndFlush(created);
        });
        if (hostelDgmAssignmentRepository.findByHostelIdAndDgmId(demoHostel.getId(), dgm.getId()).isEmpty()) {
            hostelDgmAssignmentRepository.saveAndFlush(
                    new HostelDgmAssignment(demoHostel, dgm, "DEMO_SANDBOX_INIT"));
        }
        return dgm;
    }

    private void seedSampleRecordsIfMissing(Hostel demoHostel,
                                            Student student1,
                                            Student student2,
                                            Student student3,
                                            Warden warden,
                                            Dgm dgm) {
        if (!complaintRepository.findByHostelId(demoHostel.getId()).isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Active In-Progress Complaint (assigned to demo warden)
        Complaint c1 = createDemoComplaint(
                student1,
                ComplaintType.Electricity,
                "Study desk power socket flickering intermittently",
                "The study desk wall outlet in Room D-101 sparks briefly when plugging in a laptop adapter.",
                "Room D-101, North Wing",
                ComplaintPriority.High,
                ComplaintStatus.In_Progress,
                now.plusHours(14),
                warden,
                "Electrician scheduled for North Wing Floor 1 inspection this afternoon."
        );
        c1.getTimeline().add(new ComplaintTimeline(c1, ComplaintStatus.Submitted, "Submitted by resident"));
        c1.getTimeline().add(new ComplaintTimeline(c1, ComplaintStatus.In_Progress, "Assigned to campus electrical team"));
        complaintRepository.save(c1);

        // 2. Overdue + Pending Escalation Complaint (for DGM escalations & overdue showcase)
        Complaint c2 = createDemoComplaint(
                student1,
                ComplaintType.Water,
                "Floor 1 drinking water purifier dispenser offline",
                "RO water dispenser outside Room D-101 has shown low-pressure fault indicator since yesterday evening.",
                "North Wing Floor 1 Corridor",
                ComplaintPriority.Urgent,
                ComplaintStatus.Overdue,
                now.minusHours(6),
                warden,
                "Awaiting replacement filter cartridge from vendor."
        );
        c2.setOverdueAt(now.minusHours(4));
        c2.setEscalated(true);
        c2.setEscalatedAt(now.minusHours(2));
        c2.getTimeline().add(new ComplaintTimeline(c2, ComplaintStatus.Submitted, "Submitted by resident"));
        c2.getTimeline().add(new ComplaintTimeline(c2, ComplaintStatus.Overdue, "SLA deadline exceeded"));
        c2.getTimeline().add(new ComplaintTimeline(c2, ComplaintStatus.Overdue, "Escalated to DGM for priority vendor dispatch"));
        c2 = complaintRepository.save(c2);

        ComplaintEscalation escalation = new ComplaintEscalation();
        escalation.setComplaint(c2);
        escalation.setEscalatedByStudent(student1);
        escalation.setReason("Drinking water station offline beyond Urgent SLA target");
        escalation.setSupportingDetails("Residents on Floor 1 currently need to use Floor 2 dispenser.");
        escalation.setStatus(EscalationStatus.PENDING);
        escalationRepository.save(escalation);

        // 3. Resolved Complaint (for resolution metrics showcase)
        Complaint c3 = createDemoComplaint(
                student2,
                ComplaintType.Room_Maintenance,
                "Wardrobe door hinge loose in Room D-102",
                "Right wardrobe door hinge screws loosened and door does not latch smoothly when closed.",
                "Room D-102, North Wing",
                ComplaintPriority.Medium,
                ComplaintStatus.Resolved,
                now.plusDays(1),
                warden,
                "Carpenter replaced hinge screws and verified door alignment."
        );
        c3.setResolvedAt(now.minusDays(1));
        c3.getTimeline().add(new ComplaintTimeline(c3, ComplaintStatus.Submitted, "Submitted by resident"));
        c3.getTimeline().add(new ComplaintTimeline(c3, ComplaintStatus.Resolved, "Carpentry repair completed"));
        complaintRepository.save(c3);

        // Hostel-scoped Notices (never global null hostel)
        createDemoNotice(demoHostel, "Showcase Wing Water Tank Cleaning",
                "Routine overhead tank sanitization will occur Sunday 10:00 AM to 1:00 PM in Showcase Hostel.",
                NoticeCategory.MAINTENANCE, NoticePriority.HIGH, warden.getUser().getName(), "WARDEN");
        createDemoNotice(demoHostel, "Updated Weekend Dining Hours",
                "Weekend breakfast in the Showcase Dining Hall will be served from 7:30 AM to 10:00 AM.",
                NoticeCategory.MESS, NoticePriority.MEDIUM, warden.getUser().getName(), "WARDEN");
        createDemoNotice(demoHostel, "Annual Fire Drill Advisory",
                "Scheduled evacuation drill for North and South Wings this Friday at 4:30 PM.",
                NoticeCategory.EMERGENCY, NoticePriority.URGENT, dgm.getUser().getName(), "DGM");

        // Hostel-scoped Survey + sample submissions (demoStudent left unsubmitted for UI preview)
        Survey survey = new Survey(
                "Showcase Hostel Quarterly Facilities Feedback",
                "Evaluate cleanliness, Wi-Fi reliability, water supply, and maintenance turnaround.",
                demoHostel,
                dgm.getUser().getEmail()
        );
        survey.setActive(true);
        survey.setStartDate(now.minusDays(5));
        survey.setEndDate(now.plusDays(25));
        survey = surveyRepository.saveAndFlush(survey);

        saveSubmission(survey, student2, 5, 4, 4, 4, 5, 5, 4, 5, 5, 4,
                "Quick carpentry repair response and helpful warden support.");
        saveSubmission(survey, student3, 4, 4, 4, 3, 4, 4, 4, 5, 4, 4,
                "Wi-Fi speed is good; floor water purifier maintenance can be faster.");

        // Hostel-scoped Feedback Tasks
        FeedbackTask t1 = new FeedbackTask();
        t1.setTitle("Inspect Floor 1 RO Dispenser Vendor SLA");
        t1.setDescription("Follow up with drinking water maintenance vendor based on survey comments.");
        t1.setSurvey(survey);
        t1.setHostel(demoHostel);
        t1.setWarden(warden);
        t1.setCategory("WATER");
        t1.setPriority("HIGH");
        t1.setStatus(FeedbackTaskStatus.IN_PROGRESS);
        t1.setCreatedBy(dgm.getUser().getEmail());
        feedbackTaskRepository.save(t1);

        FeedbackTask t2 = new FeedbackTask();
        t2.setTitle("Audit South Wing Locker Inventory");
        t2.setDescription("Verify locker latch repair status in Room D-202 before semester intake.");
        t2.setSurvey(survey);
        t2.setHostel(demoHostel);
        t2.setWarden(warden);
        t2.setCategory("MAINTENANCE");
        t2.setPriority("MEDIUM");
        t2.setStatus(FeedbackTaskStatus.PENDING);
        t2.setCreatedBy(dgm.getUser().getEmail());
        feedbackTaskRepository.save(t2);
    }

    private Complaint createDemoComplaint(Student student, ComplaintType type, String subject,
                                          String description, String location, ComplaintPriority priority,
                                          ComplaintStatus status, LocalDateTime slaDeadline,
                                          Warden assignedWarden, String adminNotes) {
        Complaint c = new Complaint();
        c.setStudent(student);
        c.setType(type);
        c.setSubject(subject);
        c.setDescription(description);
        c.setLocation(location);
        c.setPriority(priority);
        c.setStatus(status);
        c.setSlaDeadline(slaDeadline);
        c.setAssignedWarden(assignedWarden);
        c.setAdminNotes(adminNotes);
        c.setComplaintNumber("CMP-DEMO-TMP-" + System.nanoTime());
        c = complaintRepository.saveAndFlush(c);
        c.setComplaintNumber("CMP-DEMO-" + c.getId());
        return complaintRepository.saveAndFlush(c);
    }

    private void createDemoNotice(Hostel hostel, String title, String message,
                                  NoticeCategory category, NoticePriority priority,
                                  String authorName, String authorRole) {
        Notice notice = new Notice();
        notice.setHostel(hostel);
        notice.setTitle(title);
        notice.setMessage(message);
        notice.setCategory(category);
        notice.setPriority(priority);
        notice.setAuthorName(authorName);
        notice.setAuthorRole(authorRole);
        notice.setActive(true);
        noticeRepository.save(notice);
    }

    private void saveSubmission(Survey survey, Student student,
                                int q1, int q2, int q3, int q4, int q5,
                                int q6, int q7, int q8, int q9, int q10, String comments) {
        SurveySubmission sub = new SurveySubmission();
        sub.setSurvey(survey);
        sub.setStudent(student);
        sub.setQ1Cleanliness(q1);
        sub.setQ2Washrooms(q2);
        sub.setQ3Furniture(q3);
        sub.setQ4DrinkingWater(q4);
        sub.setQ5Wifi(q5);
        sub.setQ6Electricity(q6);
        sub.setQ7Food(q7);
        sub.setQ8Security(q8);
        sub.setQ9WardenSupport(q9);
        sub.setQ10ComplaintResolution(q10);
        sub.setComments(comments);
        surveySubmissionRepository.save(sub);
    }
}

