package com.hostelsync.backend.config;

import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

/**
 * Seeds the database with a default hostel, rooms, and demo user accounts
 * the first time the application starts against an empty database.
 *
 * This seeder is development-only: it is disabled unless
 * {@code app.seeder.enabled=true} is explicitly configured (see
 * application-local.properties / ENABLE_DATA_SEEDER). Demo credentials are
 * therefore never created by a production deployment that leaves the flag off.
 *
 * Credentials (change in production):
 *   Warden : warden.vance@hostelhub.edu / wardenPass2026
 *   Student: rahul.sharma@hostelhub.edu  / studentPass2026
 *   DGM    : dgm.director@hostelhub.edu  / dgmPass2026
 */
@Configuration
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
public class DataSeeder {

    private static final String DEMO_WARDEN_EMAIL = "warden.vance@hostelhub.edu";
    private static final String DEMO_WARDEN_PASSWORD = "wardenPass2026";
    private static final String DEMO_STUDENT_EMAIL = "rahul.sharma@hostelhub.edu";
    private static final String DEMO_STUDENT_PASSWORD = "studentPass2026";
    private static final String DEMO_DGM_EMAIL = "dgm.director@hostelhub.edu";
    private static final String DEMO_DGM_PASSWORD = "dgmPass2026";

    @Bean
    CommandLineRunner seedDatabase(
            UserRepository userRepo,
            StudentRepository studentRepo,
            WardenRepository wardenRepo,
            HostelRepository hostelRepo,
            RoomRepository roomRepo,
            DgmRepository dgmRepo,
            HostelDgmAssignmentRepository hostelDgmAssignmentRepo,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // ── Existing installation: make sure every demo login still works ──
            // Older versions seeded only Student/Warden accounts, and the DGM
            // account was added later. This branch repairs a partially seeded
            // database without touching real users or existing passwords.
            if (hostelRepo.count() > 0) {
                ensureDemoAccounts(userRepo, studentRepo, wardenRepo, hostelRepo, roomRepo,
                        dgmRepo, hostelDgmAssignmentRepo, passwordEncoder);
                return;
            }

            System.out.println(">>> DataSeeder: seeding demo data...");

            // ── Hostel ───────────────────────────────────────────────────────
            Hostel hostel = hostelRepo.save(new Hostel("HostelHub Campus Hostel", "University Campus, Main Road"));

            // ── Rooms ─────────────────────────────────────────────────────────
            Room r101  = roomRepo.save(new Room("101", "Block A", 1, 3, hostel));
            Room r204  = roomRepo.save(new Room("204", "Block A", 2, 3, hostel));
            Room r205  = roomRepo.save(new Room("205", "Block A", 2, 2, hostel));
            Room r302  = roomRepo.save(new Room("302", "Block B", 3, 4, hostel));
            Room r304  = roomRepo.save(new Room("304", "Block B", 3, 2, hostel));

            // ── Student accounts ──────────────────────────────────────────────
            createStudent(userRepo, studentRepo, passwordEncoder,
                    "Rahul Sharma",  DEMO_STUDENT_EMAIL,  DEMO_STUDENT_PASSWORD,
                    "STU-2023-089", "B.Tech Computer Science", 3, "+91 98765 43210",
                    "Male", "Manoj Sharma", "+91 98111 22334", r204);

            createStudent(userRepo, studentRepo, passwordEncoder,
                    "Aman Patel",    "aman.patel@hostelhub.edu",    "studentPass2026",
                    "STU-2023-090", "B.Tech Computer Science", 3, "+91 98765 43215",
                    "Male", "Vipin Patel", "+91 98222 33445", r204);

            createStudent(userRepo, studentRepo, passwordEncoder,
                    "Arjun Verma",   "arjun.v@hostelhub.edu",       "studentPass2026",
                    "STU-2023-014", "B.Tech CS", 2, "+91 98765 43211",
                    "Male", "Rajeev Verma", "+91 98333 44556", r101);

            createStudent(userRepo, studentRepo, passwordEncoder,
                    "Devansh Roy",   "devansh.r@hostelhub.edu",     "studentPass2026",
                    "STU-2023-045", "Mechanical Eng.", 4, "+91 98765 43216",
                    "Male", "Somen Roy", "+91 98444 55667", r205);

            createStudent(userRepo, studentRepo, passwordEncoder,
                    "Tanya Bose",    "tanya.bose@hostelhub.edu",    "studentPass2026",
                    "STU-2023-112", "B.Arch", 1, "+91 98765 43220",
                    "Female", "Subhas Bose", "+91 98555 66778", r304);

            createStudent(userRepo, studentRepo, passwordEncoder,
                    "Karan Singhania", "karan.s@hostelhub.edu",     "studentPass2026",
                    "STU-2023-046", "Civil Eng.", 4, "+91 98765 43217",
                    "Male", "Vikram Singhania", "+91 98666 77889", r205);

            createStudent(userRepo, studentRepo, passwordEncoder,
                    "Rohan Mehra",   "rohan.m@hostelhub.edu",       "studentPass2026",
                    "STU-2023-018", "B.Tech IT", 2, "+91 98765 43212",
                    "Male", "Suresh Mehra", "+91 98777 88990", r101);

            // ── Warden, student and DGM demo logins ───────────────────────────
            ensureDemoAccounts(userRepo, studentRepo, wardenRepo, hostelRepo, roomRepo,
                    dgmRepo, hostelDgmAssignmentRepo, passwordEncoder);

            System.out.println(">>> DataSeeder: done. Hostel, " + roomRepo.count()
                    + " rooms, and " + studentRepo.count() + " students seeded.");
        };
    }

    /**
     * Idempotently guarantees that the three demo logins exist and have the
     * roles/profiles expected by Spring Security. Existing user records and
     * valid password hashes are preserved; only missing records (or an
     * unusable non-BCrypt hash) are repaired.
     */
    private void ensureDemoAccounts(UserRepository userRepo,
                                    StudentRepository studentRepo,
                                    WardenRepository wardenRepo,
                                    HostelRepository hostelRepo,
                                    RoomRepository roomRepo,
                                    DgmRepository dgmRepo,
                                    HostelDgmAssignmentRepository hostelDgmAssignmentRepo,
                                    PasswordEncoder passwordEncoder) {

        User wardenUser = ensureUser(userRepo, passwordEncoder,
                "Dr. Arthur Vance", DEMO_WARDEN_EMAIL, DEMO_WARDEN_PASSWORD, UserRole.WARDEN);
        ensureWardenProfile(wardenRepo, hostelRepo, wardenUser);

        User studentUser = ensureUser(userRepo, passwordEncoder,
                "Rahul Sharma", DEMO_STUDENT_EMAIL, DEMO_STUDENT_PASSWORD, UserRole.STUDENT);
        ensureStudentProfile(studentRepo, roomRepo, studentUser);

        User dgmUser = ensureUser(userRepo, passwordEncoder,
                "Dr. Rajesh Verma", DEMO_DGM_EMAIL, DEMO_DGM_PASSWORD, UserRole.DGM);
        ensureDgmProfile(dgmRepo, hostelDgmAssignmentRepo, hostelRepo, dgmUser);
    }

    private User ensureUser(UserRepository userRepo,
                            PasswordEncoder passwordEncoder,
                            String name, String email, String configuredPassword,
                            UserRole expectedRole) {

        Optional<User> existing = userRepo.findByEmailIgnoreCase(email);
        if (existing.isEmpty()) {
            User user = new User(name, email, passwordEncoder.encode(configuredPassword), expectedRole);
            user = userRepo.save(user);
            System.out.println(">>> DataSeeder: created demo account " + email + " (" + expectedRole + ").");
            return user;
        }

        User user = existing.get();
        boolean changed = false;

        // The demo email is a fixed development identity. Keep the role aligned
        // with the role the frontend and SecurityConfig expect.
        if (user.getRole() != expectedRole) {
            user.setRole(expectedRole);
            changed = true;
            System.out.println(">>> DataSeeder: corrected role for demo account " + email
                    + " to " + expectedRole + ".");
        }

        // Never replace a valid existing password. If the stored value is not a
        // usable BCrypt hash, repair it with the configured demo password.
        if (!isBcryptHash(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(configuredPassword));
            changed = true;
            System.out.println(">>> DataSeeder: repaired invalid password hash for demo account " + email + ".");
        }

        return changed ? userRepo.save(user) : user;
    }

    private void ensureWardenProfile(WardenRepository wardenRepo,
                                     HostelRepository hostelRepo,
                                     User wardenUser) {
        if (wardenRepo.findByUser(wardenUser).isPresent()) {
            return;
        }
        Hostel hostel = hostelRepo.findAll().stream().findFirst().orElse(null);
        wardenRepo.save(new Warden(wardenUser, "WDN-0042", hostel, "+91 98765 43210"));
        System.out.println(">>> DataSeeder: created Warden profile for " + wardenUser.getEmail() + ".");
    }

    private void ensureStudentProfile(StudentRepository studentRepo,
                                      RoomRepository roomRepo,
                                      User studentUser) {
        if (studentRepo.findByUser(studentUser).isPresent()) {
            return;
        }
        if (studentRepo.existsByStudentId("STU-2023-089")) {
            System.out.println(">>> DataSeeder: student ID STU-2023-089 is already used by another record; "
                    + "skipping demo Student profile creation.");
            return;
        }

        Student student = new Student();
        student.setUser(studentUser);
        student.setStudentId("STU-2023-089");
        student.setCourse("B.Tech Computer Science");
        student.setYear(3);
        student.setPhone("+91 98765 43210");
        student.setGender("Male");
        student.setGuardianName("Manoj Sharma");
        student.setGuardianPhone("+91 98111 22334");
        student.setStatus(StudentStatus.Active);
        student.setRoom(roomRepo.findAll().stream().findFirst().orElse(null));
        studentRepo.save(student);
        System.out.println(">>> DataSeeder: created Student profile for " + studentUser.getEmail() + ".");
    }

    private void ensureDgmProfile(DgmRepository dgmRepo,
                                  HostelDgmAssignmentRepository hostelDgmAssignmentRepo,
                                  HostelRepository hostelRepo,
                                  User dgmUser) {

        Dgm dgm = dgmRepo.findByUserId(dgmUser.getId()).orElseGet(() -> {
            Dgm created = new Dgm(dgmUser, "DGM-HQ-01", "Director General of Hostels");
            created.setOfficeLocation("Central Administrative Block, Room 102");
            created.setContactExtension("x4401");
            System.out.println(">>> DataSeeder: created DGM profile for " + dgmUser.getEmail() + ".");
            return dgmRepo.save(created);
        });

        for (Hostel h : hostelRepo.findAll()) {
            if (hostelDgmAssignmentRepo.findByHostelIdAndDgmId(h.getId(), dgm.getId()).isEmpty()) {
                hostelDgmAssignmentRepo.save(new HostelDgmAssignment(h, dgm, "SYSTEM_INIT"));
            }
        }
    }

    private boolean isBcryptHash(String hash) {
        return hash != null
                && hash.length() == 60
                && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    private void createStudent(UserRepository userRepo,
                                StudentRepository studentRepo,
                                PasswordEncoder passwordEncoder,
                                String name, String email, String password,
                                String studentId, String course, int year, String phone,
                                String gender, String guardianName, String guardianPhone,
                                Room room) {
        if (userRepo.existsByEmail(email)) return;

        User user = new User(name, email, passwordEncoder.encode(password), UserRole.STUDENT);
        user = userRepo.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentId(studentId);
        student.setCourse(course);
        student.setYear(year);
        student.setPhone(phone);
        student.setGender(gender);
        student.setGuardianName(guardianName);
        student.setGuardianPhone(guardianPhone);
        student.setStatus(StudentStatus.Active);
        student.setRoom(room);

        studentRepo.save(student);
    }
}
