package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.NoticeCreateRequest;
import com.hostelsync.backend.dto.NoticeResponse;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.HostelRepository;
import com.hostelsync.backend.repository.NoticeRepository;
import com.hostelsync.backend.repository.StudentRepository;
import com.hostelsync.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final HostelRepository hostelRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private DemoScopeService demoScopeService;

    public NoticeService(NoticeRepository noticeRepository,
                         HostelRepository hostelRepository,
                         StudentRepository studentRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.noticeRepository = noticeRepository;
        this.hostelRepository = hostelRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Autowired(required = false)
    public void setDemoScopeService(DemoScopeService demoScopeService) {
        this.demoScopeService = demoScopeService;
    }

    public NoticeResponse createNotice(NoticeCreateRequest request, String authorEmail, String authorRole) {
        User author = userRepository.findByEmail(authorEmail).orElse(null);
        String authorName = author != null ? author.getName() : "Administrator";

        Hostel hostel = null;
        if (request.getHostelId() != null) {
            if (demoScopeService != null) {
                demoScopeService.assertDemoHostelAccess(request.getHostelId());
            }
            hostel = hostelRepository.findById(request.getHostelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hostel", request.getHostelId()));
        }

        NoticeCategory cat;
        try {
            cat = NoticeCategory.valueOf(request.getCategory().toUpperCase());
        } catch (Exception e) {
            cat = NoticeCategory.GENERAL;
        }

        NoticePriority prio;
        try {
            prio = NoticePriority.valueOf(request.getPriority().toUpperCase());
        } catch (Exception e) {
            prio = NoticePriority.MEDIUM;
        }

        Notice notice = new Notice();
        notice.setTitle(request.getTitle().trim());
        notice.setMessage(request.getMessage().trim());
        notice.setCategory(cat);
        notice.setPriority(prio);
        notice.setHostel(hostel);
        notice.setAuthorName(authorName);
        notice.setAuthorRole(authorRole != null ? authorRole : "STAFF");
        notice.setExpiresAt(request.getExpiresAt());
        notice.setActive(true);

        notice = noticeRepository.save(notice);

        // Notify students
        List<Student> students = (hostel != null)
                ? studentRepository.findAll()
                : studentRepository.findAll();

        for (Student s : students) {
            if (s.getStatus() == StudentStatus.Active && (hostel == null || (s.getRoom() != null && s.getRoom().getHostel().getId().equals(hostel.getId())))) {
                notificationService.notifyGeneral(
                        s.getUser().getId(),
                        "Notice: " + notice.getTitle(),
                        notice.getMessage().length() > 80 ? notice.getMessage().substring(0, 77) + "..." : notice.getMessage()
                );
            }
        }

        return toResponse(notice);
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getActiveNotices(String userEmail) {
        LocalDateTime now = LocalDateTime.now();
        Student student = studentRepository.findByUserEmail(userEmail).orElse(null);

        List<Notice> list;
        if (student != null && student.getRoom() != null && student.getRoom().getHostel() != null) {
            list = noticeRepository.findActiveNoticesForHostel(student.getRoom().getHostel().getId(), now);
        } else {
            list = noticeRepository.findAllActiveNotices(now);
        }

        return list.stream()
                .filter(n -> demoScopeService == null || demoScopeService.isNoticeAccessible(n))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(n -> demoScopeService == null || demoScopeService.isNoticeAccessible(n))
                .map(this::toResponse)
                .toList();
    }

    public NoticeResponse toggleNoticeActive(Long id, boolean active) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
        if (demoScopeService != null) {
            demoScopeService.assertNoticeAccessible(notice);
        }
        notice.setActive(active);
        return toResponse(noticeRepository.save(notice));
    }

    private NoticeResponse toResponse(Notice n) {
        NoticeResponse res = new NoticeResponse();
        res.setId(n.getId());
        res.setTitle(n.getTitle());
        res.setMessage(n.getMessage());
        res.setCategory(n.getCategory().name());
        res.setPriority(n.getPriority().name());
        if (n.getHostel() != null) {
            res.setHostelId(n.getHostel().getId());
            res.setHostelName(n.getHostel().getName());
        }
        res.setActive(n.isActive());
        res.setAuthorName(n.getAuthorName());
        res.setAuthorRole(n.getAuthorRole());
        res.setCreatedAt(n.getCreatedAt());
        res.setExpiresAt(n.getExpiresAt());
        return res;
    }
}
