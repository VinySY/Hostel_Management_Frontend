package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.*;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveySubmissionRepository submissionRepository;
    private final HostelRepository hostelRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;

    public SurveyService(SurveyRepository surveyRepository,
                         SurveySubmissionRepository submissionRepository,
                         HostelRepository hostelRepository,
                         StudentRepository studentRepository,
                         NotificationService notificationService) {
        this.surveyRepository = surveyRepository;
        this.submissionRepository = submissionRepository;
        this.hostelRepository = hostelRepository;
        this.studentRepository = studentRepository;
        this.notificationService = notificationService;
    }

    public SurveyResponse createSurvey(SurveyCreateRequest request, String createdBy) {
        Hostel hostel = null;
        if (request.getTargetHostelId() != null) {
            hostel = hostelRepository.findById(request.getTargetHostelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hostel", request.getTargetHostelId()));
        }

        Survey survey = new Survey(request.getTitle().trim(), request.getDescription(), hostel, createdBy);
        if (request.getStartDate() != null) survey.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) survey.setEndDate(request.getEndDate());
        survey = surveyRepository.save(survey);

        // Notify students
        List<Student> students = (hostel != null)
                ? studentRepository.findByRoomHostelIdAndStatus(hostel.getId(), StudentStatus.Active)
                : studentRepository.findAll();

        for (Student s : students) {
            if (s.getStatus() == StudentStatus.Active && (hostel == null || (s.getRoom() != null && s.getRoom().getHostel().getId().equals(hostel.getId())))) {
                notificationService.notifyGeneral(
                        s.getUser().getId(),
                        "New Survey: " + survey.getTitle(),
                        "Director General of Hostels has published a student feedback survey. Please share your feedback."
                );
            }
        }

        return toSurveyResponse(survey, null);
    }

    @Transactional(readOnly = true)
    public List<SurveyResponse> getAllSurveys() {
        return surveyRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(s -> toSurveyResponse(s, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SurveyResponse> getActiveSurveysForStudent(String studentEmail) {
        Student student = studentRepository.findByUserEmail(studentEmail)
                .orElse(null);

        Long hostelId = (student != null && student.getRoom() != null && student.getRoom().getHostel() != null)
                ? student.getRoom().getHostel().getId()
                : null;

        List<Survey> surveys = (hostelId != null)
                ? surveyRepository.findActiveSurveysForHostel(hostelId)
                : surveyRepository.findByActiveTrueOrderByCreatedAtDesc();

        return surveys.stream()
                .map(s -> toSurveyResponse(s, student != null ? student.getId() : null))
                .toList();
    }

    public void submitSurvey(Long surveyId, SurveySubmissionRequest request, String studentEmail) {
        Student student = studentRepository.findByUserEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for " + studentEmail));

        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", surveyId));

        if (!survey.isActive()) {
            throw new IllegalArgumentException("This survey is no longer active");
        }

        if (submissionRepository.existsBySurveyIdAndStudentId(surveyId, student.getId())) {
            throw new IllegalArgumentException("You have already submitted feedback for this survey");
        }

        // Enforce 1-5 rating bounds for all 10 questions
        validateRatings(request);

        // Enforce 30-word limit on written feedback
        if (request.getComments() != null && !request.getComments().isBlank()) {
            String[] words = request.getComments().trim().split("\\s+");
            if (words.length > 30) {
                throw new IllegalArgumentException("Feedback comments cannot exceed 30 words (found " + words.length + " words)");
            }
        }

        SurveySubmission sub = new SurveySubmission();
        sub.setSurvey(survey);
        sub.setStudent(student);
        sub.setQ1Cleanliness(request.getQ1Cleanliness());
        sub.setQ2Washrooms(request.getQ2Washrooms());
        sub.setQ3Furniture(request.getQ3Furniture());
        sub.setQ4DrinkingWater(request.getQ4DrinkingWater());
        sub.setQ5Wifi(request.getQ5Wifi());
        sub.setQ6Electricity(request.getQ6Electricity());
        sub.setQ7Food(request.getQ7Food());
        sub.setQ8Security(request.getQ8Security());
        sub.setQ9WardenSupport(request.getQ9WardenSupport());
        sub.setQ10ComplaintResolution(request.getQ10ComplaintResolution());
        sub.setComments(request.getComments() != null ? request.getComments().trim() : null);

        submissionRepository.save(sub);
    }

    private static void validateRatings(SurveySubmissionRequest request) {
        int[] ratings = {
                request.getQ1Cleanliness(),
                request.getQ2Washrooms(),
                request.getQ3Furniture(),
                request.getQ4DrinkingWater(),
                request.getQ5Wifi(),
                request.getQ6Electricity(),
                request.getQ7Food(),
                request.getQ8Security(),
                request.getQ9WardenSupport(),
                request.getQ10ComplaintResolution()
        };
        for (int rating : ratings) {
            if (rating < 1 || rating > 5) {
                throw new BadRequestException("Rating must be between 1 and 5");
            }
        }
    }

    @Transactional(readOnly = true)
    public SurveyAnalyticsResponse getSurveyAnalytics(Long surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", surveyId));

        List<SurveySubmission> subs = submissionRepository.findBySurveyIdOrderBySubmittedAtDesc(surveyId);

        SurveyAnalyticsResponse res = new SurveyAnalyticsResponse();
        res.setSurveyId(survey.getId());
        res.setSurveyTitle(survey.getTitle());
        res.setTotalResponses(subs.size());

        if (subs.isEmpty()) {
            res.setOverallSatisfactionScore(0.0);
            return res;
        }

        double q1 = subs.stream().mapToInt(SurveySubmission::getQ1Cleanliness).average().orElse(0.0);
        double q2 = subs.stream().mapToInt(SurveySubmission::getQ2Washrooms).average().orElse(0.0);
        double q3 = subs.stream().mapToInt(SurveySubmission::getQ3Furniture).average().orElse(0.0);
        double q4 = subs.stream().mapToInt(SurveySubmission::getQ4DrinkingWater).average().orElse(0.0);
        double q5 = subs.stream().mapToInt(SurveySubmission::getQ5Wifi).average().orElse(0.0);
        double q6 = subs.stream().mapToInt(SurveySubmission::getQ6Electricity).average().orElse(0.0);
        double q7 = subs.stream().mapToInt(SurveySubmission::getQ7Food).average().orElse(0.0);
        double q8 = subs.stream().mapToInt(SurveySubmission::getQ8Security).average().orElse(0.0);
        double q9 = subs.stream().mapToInt(SurveySubmission::getQ9WardenSupport).average().orElse(0.0);
        double q10 = subs.stream().mapToInt(SurveySubmission::getQ10ComplaintResolution).average().orElse(0.0);

        res.setQ1CleanlinessAvg(Math.round(q1 * 10.0) / 10.0);
        res.setQ2WashroomsAvg(Math.round(q2 * 10.0) / 10.0);
        res.setQ3FurnitureAvg(Math.round(q3 * 10.0) / 10.0);
        res.setQ4DrinkingWaterAvg(Math.round(q4 * 10.0) / 10.0);
        res.setQ5WifiAvg(Math.round(q5 * 10.0) / 10.0);
        res.setQ6ElectricityAvg(Math.round(q6 * 10.0) / 10.0);
        res.setQ7FoodAvg(Math.round(q7 * 10.0) / 10.0);
        res.setQ8SecurityAvg(Math.round(q8 * 10.0) / 10.0);
        res.setQ9WardenSupportAvg(Math.round(q9 * 10.0) / 10.0);
        res.setQ10ComplaintResolutionAvg(Math.round(q10 * 10.0) / 10.0);

        double totalAvg = (q1 + q2 + q3 + q4 + q5 + q6 + q7 + q8 + q9 + q10) / 10.0;
        double satisfactionPercentage = (totalAvg / 5.0) * 100.0;
        res.setOverallSatisfactionScore(Math.round(satisfactionPercentage * 10.0) / 10.0);

        List<SurveyCommentDto> comments = subs.stream()
                .filter(s -> s.getComments() != null && !s.getComments().isBlank())
                .map(s -> new SurveyCommentDto(
                        s.getId(),
                        s.getStudent().getUser().getName(),
                        s.getStudent().getStudentId(),
                        s.getStudent().getRoom() != null && s.getStudent().getRoom().getHostel() != null ? s.getStudent().getRoom().getHostel().getName() : "Hostel",
                        s.getStudent().getRoom() != null ? s.getStudent().getRoom().getRoomNumber() : "-",
                        s.getComments(),
                        s.getSubmittedAt()
                ))
                .toList();
        res.setComments(comments);

        return res;
    }

    public SurveyResponse toggleSurveyActive(Long surveyId, boolean active) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", surveyId));
        survey.setActive(active);
        return toSurveyResponse(surveyRepository.save(survey), null);
    }

    private SurveyResponse toSurveyResponse(Survey s, Long currentStudentId) {
        SurveyResponse res = new SurveyResponse();
        res.setId(s.getId());
        res.setTitle(s.getTitle());
        res.setDescription(s.getDescription());
        if (s.getTargetHostel() != null) {
            res.setTargetHostelId(s.getTargetHostel().getId());
            res.setTargetHostelName(s.getTargetHostel().getName());
        }
        res.setActive(s.isActive());
        res.setStartDate(s.getStartDate());
        res.setEndDate(s.getEndDate());
        res.setCreatedBy(s.getCreatedBy());
        res.setCreatedAt(s.getCreatedAt());
        res.setResponseCount(submissionRepository.countBySurveyId(s.getId()));

        if (currentStudentId != null) {
            res.setHasSubmitted(submissionRepository.existsBySurveyIdAndStudentId(s.getId(), currentStudentId));
        }

        return res;
    }
}
