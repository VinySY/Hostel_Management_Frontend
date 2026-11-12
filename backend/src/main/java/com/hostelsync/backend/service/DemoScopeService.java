package com.hostelsync.backend.service;

import com.hostelsync.backend.config.DemoAccountRegistry;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Central bidirectional demo-isolation policy helper used across services, controllers, and jobs
 * to keep Demo Mode identities and institutional users strictly separated.
 */
@Service
@Transactional(readOnly = true)
public class DemoScopeService {

    private static final String DEMO_ACCESS_DENIED_MSG =
            "Demo accounts can only access demo showcase records.";
    private static final String INSTITUTIONAL_ACCESS_DENIED_MSG =
            "Institutional accounts cannot access or modify demo showcase records.";
    private static final String CROSS_ENV_ASSIGNMENT_MSG =
            "Cross-environment assignments between demo and institutional records are not allowed.";

    private final DemoAccountRegistry demoAccountRegistry;
    private final HostelRepository hostelRepository;
    private final ComplaintRepository complaintRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final WardenRepository wardenRepository;
    private final UserRepository userRepository;

    public DemoScopeService(DemoAccountRegistry demoAccountRegistry,
                            HostelRepository hostelRepository,
                            ComplaintRepository complaintRepository,
                            StudentRepository studentRepository,
                            RoomRepository roomRepository,
                            WardenRepository wardenRepository,
                            UserRepository userRepository) {
        this.demoAccountRegistry = demoAccountRegistry;
        this.hostelRepository = hostelRepository;
        this.complaintRepository = complaintRepository;
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.wardenRepository = wardenRepository;
        this.userRepository = userRepository;
    }

    public boolean isCurrentCallerDemo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        String name = auth.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return false;
        }
        return demoAccountRegistry.isDemoEmail(name);
    }

    public boolean isDemoEmail(String email) {
        return demoAccountRegistry.isDemoEmail(email);
    }

    public boolean isDemoHostelName(String hostelName) {
        return demoAccountRegistry.isDemoHostelName(hostelName);
    }

    public void assertNotDemoEmail(String email) {
        if (demoAccountRegistry.isDemoEmail(email)) {
            throw new ForbiddenException("Reserved demo email domain cannot be used for institutional accounts.");
        }
    }

    public void assertNotDemoHostelName(String hostelName) {
        if (demoAccountRegistry.isDemoHostelName(hostelName)) {
            throw new ForbiddenException("Reserved demo hostel name cannot be used for institutional hostels.");
        }
    }

    public Optional<Hostel> findDemoHostel() {
        return hostelRepository.findByName(demoAccountRegistry.getDemoHostelName());
    }

    public Long requireDemoHostelId() {
        return findDemoHostel()
                .map(Hostel::getId)
                .orElseThrow(() -> new ForbiddenException("Demo showcase hostel is not configured."));
    }

    public boolean isInDemoHostel(Long hostelId) {
        if (hostelId == null) {
            return false;
        }
        return findDemoHostel().map(h -> h.getId().equals(hostelId)).orElse(false);
    }

    public boolean isDemoHostel(Hostel hostel) {
        return hostel != null && (isInDemoHostel(hostel.getId()) || demoAccountRegistry.isDemoHostelName(hostel.getName()));
    }

    private void assertSameEnvironment(boolean targetIsDemo) {
        boolean callerIsDemo = isCurrentCallerDemo();
        if (callerIsDemo && !targetIsDemo) {
            throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
        }
        if (!callerIsDemo && targetIsDemo) {
            throw new ForbiddenException(INSTITUTIONAL_ACCESS_DENIED_MSG);
        }
    }

    public void assertSameEnvironmentPair(boolean firstIsDemo, boolean secondIsDemo) {
        if (firstIsDemo != secondIsDemo) {
            throw new ForbiddenException(CROSS_ENV_ASSIGNMENT_MSG);
        }
    }

    public void assertDemoHostelAccess(Long hostelId) {
        if (hostelId == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isInDemoHostel(hostelId));
    }

    public boolean isHostelAccessible(Long hostelId) {
        return hostelId != null && isCurrentCallerDemo() == isInDemoHostel(hostelId);
    }

    public boolean isRoomInDemoHostel(Room room) {
        return room != null && isDemoHostel(room.getHostel());
    }

    public boolean isRoomAccessible(Room room) {
        return room != null && isCurrentCallerDemo() == isRoomInDemoHostel(room);
    }

    public void assertRoomAccessible(Room room) {
        if (room == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isRoomInDemoHostel(room));
    }

    public boolean isStudentInDemoHostel(Student student) {
        return student != null
                && (isRoomInDemoHostel(student.getRoom())
                || (student.getUser() != null && isDemoEmail(student.getUser().getEmail())));
    }

    public boolean isStudentAccessible(Student student) {
        return student != null && isCurrentCallerDemo() == isStudentInDemoHostel(student);
    }

    public void assertStudentAccessible(Student student) {
        if (student == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isStudentInDemoHostel(student));
    }

    public boolean isWardenInDemoHostel(Warden warden) {
        return warden != null
                && (isDemoHostel(warden.getHostel())
                || (warden.getUser() != null && isDemoEmail(warden.getUser().getEmail())));
    }

    public boolean isWardenAccessible(Warden warden) {
        return warden != null && isCurrentCallerDemo() == isWardenInDemoHostel(warden);
    }

    public void assertWardenAccessible(Warden warden) {
        if (warden == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isWardenInDemoHostel(warden));
    }

    public boolean isDgmInDemoEnvironment(Dgm dgm) {
        return dgm != null && dgm.getUser() != null && isDemoEmail(dgm.getUser().getEmail());
    }

    public boolean isDgmAccessible(Dgm dgm) {
        return dgm != null && isCurrentCallerDemo() == isDgmInDemoEnvironment(dgm);
    }

    public void assertDgmAccessible(Dgm dgm) {
        if (dgm == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isDgmInDemoEnvironment(dgm));
    }

    public boolean isComplaintInDemoHostel(Complaint complaint) {
        return complaint != null
                && (isStudentInDemoHostel(complaint.getStudent())
                || isWardenInDemoHostel(complaint.getAssignedWarden()));
    }

    public boolean isComplaintAccessible(Complaint complaint) {
        return complaint != null && isCurrentCallerDemo() == isComplaintInDemoHostel(complaint);
    }

    public void assertComplaintAccessible(Complaint complaint) {
        if (complaint == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isComplaintInDemoHostel(complaint));
    }

    public void assertComplaintIdAccessible(Long complaintId) {
        if (complaintId == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isEmpty()) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isComplaintInDemoHostel(complaintOpt.get()));
    }

    public void assertDemoUserIdAccess(Long userId) {
        if (userId == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        Optional<User> targetOpt = userRepository.findById(userId);
        if (targetOpt.isEmpty()) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isDemoAccessibleUser(targetOpt.get()));
    }

    public boolean isDemoAccessibleUser(User target) {
        if (target == null) {
            return false;
        }
        if (isDemoEmail(target.getEmail())) {
            return true;
        }
        if (target.getRole() == UserRole.STUDENT) {
            return studentRepository.findByUserId(target.getId())
                    .map(this::isStudentInDemoHostel)
                    .orElse(false);
        }
        if (target.getRole() == UserRole.WARDEN) {
            return wardenRepository.findByUserId(target.getId())
                    .map(this::isWardenInDemoHostel)
                    .orElse(false);
        }
        return false;
    }

    public void assertAvatarAccessible(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        String avatarPath = "/api/files/" + storedFileName;
        Optional<User> ownerOpt = userRepository.findByAvatarUrl(avatarPath);
        if (ownerOpt.isEmpty()) {
            ownerOpt = userRepository.findByAvatarUrl(storedFileName);
        }
        if (isCurrentCallerDemo()) {
            User owner = ownerOpt.orElseThrow(() -> new ForbiddenException(DEMO_ACCESS_DENIED_MSG));
            if (!isDemoAccessibleUser(owner)) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
        } else if (ownerOpt.isPresent() && isDemoAccessibleUser(ownerOpt.get())) {
            throw new ForbiddenException(INSTITUTIONAL_ACCESS_DENIED_MSG);
        }
    }

    public void assertAuditEntityAccessible(String entityType, Long entityId) {
        if (entityType == null || entityId == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        if ("Complaint".equalsIgnoreCase(entityType)) {
            assertComplaintIdAccessible(entityId);
            return;
        }
        if ("User".equalsIgnoreCase(entityType)) {
            assertDemoUserIdAccess(entityId);
            return;
        }
        if ("Student".equalsIgnoreCase(entityType)) {
            Student s = studentRepository.findById(entityId).orElse(null);
            assertStudentAccessible(s);
            return;
        }
        if ("Room".equalsIgnoreCase(entityType)) {
            Room r = roomRepository.findById(entityId).orElse(null);
            assertRoomAccessible(r);
            return;
        }
        if ("Hostel".equalsIgnoreCase(entityType)) {
            assertDemoHostelAccess(entityId);
            return;
        }
        if ("Warden".equalsIgnoreCase(entityType)) {
            Warden w = wardenRepository.findById(entityId).orElse(null);
            assertWardenAccessible(w);
            return;
        }
        if (isCurrentCallerDemo()) {
            throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
        }
    }

    public boolean isNoticeInDemoHostel(Notice notice) {
        return notice != null && isDemoHostel(notice.getHostel());
    }

    public boolean isNoticeAccessible(Notice notice) {
        if (notice == null) {
            return false;
        }
        boolean targetDemo = isNoticeInDemoHostel(notice);
        return isCurrentCallerDemo() ? targetDemo : !targetDemo;
    }

    public void assertNoticeAccessible(Notice notice) {
        if (notice == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isNoticeInDemoHostel(notice));
    }

    public boolean isSurveyInDemoHostel(Survey survey) {
        return survey != null && isDemoHostel(survey.getTargetHostel());
    }

    public boolean isSurveyAccessible(Survey survey) {
        if (survey == null) {
            return false;
        }
        boolean targetDemo = isSurveyInDemoHostel(survey);
        return isCurrentCallerDemo() ? targetDemo : !targetDemo;
    }

    public void assertSurveyAccessible(Survey survey) {
        if (survey == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isSurveyInDemoHostel(survey));
    }

    public boolean isFeedbackTaskInDemoHostel(FeedbackTask task) {
        return task != null && (isDemoHostel(task.getHostel()) || isWardenInDemoHostel(task.getWarden()));
    }

    public boolean isFeedbackTaskAccessible(FeedbackTask task) {
        return task != null && isCurrentCallerDemo() == isFeedbackTaskInDemoHostel(task);
    }

    public void assertFeedbackTaskAccessible(FeedbackTask task) {
        if (task == null) {
            if (isCurrentCallerDemo()) {
                throw new ForbiddenException(DEMO_ACCESS_DENIED_MSG);
            }
            return;
        }
        assertSameEnvironment(isFeedbackTaskInDemoHostel(task));
    }
}
