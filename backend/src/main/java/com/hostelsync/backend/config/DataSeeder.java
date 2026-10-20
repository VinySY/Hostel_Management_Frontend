package com.hostelsync.backend.config;

import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the database with a default hostel, rooms, and demo user accounts
 * the first time the application starts against an empty database.
 *
 * Credentials (change in production):
 *   Warden : warden.vance@hostelhub.edu / wardenPass2026
 *   Student: rahul.sharma@hostelhub.edu  / studentPass2026
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedDatabase(
            UserRepository userRepo,
            StudentRepository studentRepo,
            WardenRepository wardenRepo,
            HostelRepository hostelRepo,
            RoomRepository roomRepo,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // ── Only seed if nothing exists yet ─────────────────────────────
            if (hostelRepo.count() > 0) {
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

            // ── Warden account ────────────────────────────────────────────────
            User wardenUser = new User(
                    "Dr. Arthur Vance",
                    "warden.vance@hostelhub.edu",
                    passwordEncoder.encode("wardenPass2026"),
                    UserRole.WARDEN);
            wardenUser = userRepo.save(wardenUser);
            wardenRepo.save(new Warden(wardenUser, "WDN-0042"));

            // ── Student accounts ──────────────────────────────────────────────
            createStudent(userRepo, studentRepo, passwordEncoder,
                    "Rahul Sharma",  "rahul.sharma@hostelhub.edu",  "studentPass2026",
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

            System.out.println(">>> DataSeeder: done. Hostel, " + roomRepo.count()
                    + " rooms, and " + studentRepo.count() + " students seeded.");
        };
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
