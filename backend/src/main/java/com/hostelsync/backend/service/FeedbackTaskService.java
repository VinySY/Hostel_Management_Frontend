package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.FeedbackTaskCreateRequest;
import com.hostelsync.backend.dto.FeedbackTaskResponse;
import com.hostelsync.backend.dto.FeedbackTaskUpdateRequest;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.FeedbackTaskRepository;
import com.hostelsync.backend.repository.HostelRepository;
import com.hostelsync.backend.repository.SurveyRepository;
import com.hostelsync.backend.repository.WardenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class FeedbackTaskService {

    private final FeedbackTaskRepository feedbackTaskRepository;
    private final HostelRepository hostelRepository;
    private final WardenRepository wardenRepository;
    private final SurveyRepository surveyRepository;
    private final NotificationService notificationService;
    private DemoScopeService demoScopeService;

    public FeedbackTaskService(FeedbackTaskRepository feedbackTaskRepository,
                               HostelRepository hostelRepository,
                               WardenRepository wardenRepository,
                               SurveyRepository surveyRepository,
                               NotificationService notificationService) {
        this.feedbackTaskRepository = feedbackTaskRepository;
        this.hostelRepository = hostelRepository;
        this.wardenRepository = wardenRepository;
        this.surveyRepository = surveyRepository;
        this.notificationService = notificationService;
    }

    @Autowired(required = false)
    public void setDemoScopeService(DemoScopeService demoScopeService) {
        this.demoScopeService = demoScopeService;
    }

    public FeedbackTaskResponse createTask(FeedbackTaskCreateRequest request, String createdBy) {
        Hostel hostel = hostelRepository.findById(request.getHostelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", request.getHostelId()));

        Warden warden = wardenRepository.findById(request.getWardenId())
                .orElseThrow(() -> new ResourceNotFoundException("Warden", request.getWardenId()));

        if (demoScopeService != null) {
            demoScopeService.assertDemoHostelAccess(request.getHostelId());
            demoScopeService.assertWardenAccessible(warden);
        }

        Survey survey = null;
        if (request.getSurveyId() != null) {
            survey = surveyRepository.findById(request.getSurveyId()).orElse(null);
        }

        FeedbackTask task = new FeedbackTask();
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription().trim());
        task.setHostel(hostel);
        task.setWarden(warden);
        task.setSurvey(survey);
        task.setCategory(request.getCategory() != null ? request.getCategory() : "GENERAL");
        task.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        task.setStatus(FeedbackTaskStatus.PENDING);
        task.setCreatedBy(createdBy != null ? createdBy : "DGM");

        task = feedbackTaskRepository.save(task);

        notificationService.notifyGeneral(
                warden.getUser().getId(),
                "New Action Task: " + task.getTitle(),
                "DGM has assigned an actionable feedback task: " + task.getTitle()
        );

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<FeedbackTaskResponse> getTasksForWarden(String wardenEmail) {
        Warden warden = wardenRepository.findAll()
                .stream()
                .filter(w -> w.getUser().getEmail().equalsIgnoreCase(wardenEmail))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Warden not found for " + wardenEmail));

        return feedbackTaskRepository.findByWardenIdOrderByCreatedAtDesc(warden.getId())
                .stream()
                .filter(t -> demoScopeService == null || demoScopeService.isFeedbackTaskAccessible(t))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackTaskResponse> getAllTasks(Long hostelId) {
        if (demoScopeService != null && demoScopeService.isCurrentCallerDemo()) {
            Long demoHostelId = demoScopeService.requireDemoHostelId();
            if (hostelId != null) {
                demoScopeService.assertDemoHostelAccess(hostelId);
            }
            return feedbackTaskRepository.findByHostelIdOrderByCreatedAtDesc(demoHostelId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        if (demoScopeService != null && hostelId != null) {
            demoScopeService.assertDemoHostelAccess(hostelId);
        }

        List<FeedbackTask> list = (hostelId != null)
                ? feedbackTaskRepository.findByHostelIdOrderByCreatedAtDesc(hostelId)
                : feedbackTaskRepository.findAllByOrderByCreatedAtDesc();

        if (demoScopeService != null) {
            list = list.stream().filter(demoScopeService::isFeedbackTaskAccessible).toList();
        }

        return list.stream().map(this::toResponse).toList();
    }

    public FeedbackTaskResponse updateTaskStatus(Long id, FeedbackTaskUpdateRequest request, String updatedBy) {
        FeedbackTask task = feedbackTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeedbackTask", id));

        if (demoScopeService != null) {
            demoScopeService.assertFeedbackTaskAccessible(task);
        }

        FeedbackTaskStatus newStatus;
        try {
            newStatus = FeedbackTaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status: " + request.getStatus());
        }

        task.setStatus(newStatus);
        if (request.getResolutionNotes() != null) {
            task.setResolutionNotes(request.getResolutionNotes().trim());
        }
        if (newStatus == FeedbackTaskStatus.RESOLVED) {
            task.setResolvedAt(LocalDateTime.now());
        }

        task = feedbackTaskRepository.save(task);
        return toResponse(task);
    }

    private FeedbackTaskResponse toResponse(FeedbackTask t) {
        FeedbackTaskResponse res = new FeedbackTaskResponse();
        res.setId(t.getId());
        res.setTitle(t.getTitle());
        res.setDescription(t.getDescription());
        if (t.getSurvey() != null) res.setSurveyId(t.getSurvey().getId());
        res.setHostelId(t.getHostel().getId());
        res.setHostelName(t.getHostel().getName());
        res.setWardenId(t.getWarden().getId());
        res.setWardenName(t.getWarden().getUser().getName());
        res.setCategory(t.getCategory());
        res.setPriority(t.getPriority());
        res.setStatus(t.getStatus().name());
        res.setResolutionNotes(t.getResolutionNotes());
        res.setResolvedAt(t.getResolvedAt());
        res.setCreatedBy(t.getCreatedBy());
        res.setCreatedAt(t.getCreatedAt());
        res.setUpdatedAt(t.getUpdatedAt());
        return res;
    }
}
