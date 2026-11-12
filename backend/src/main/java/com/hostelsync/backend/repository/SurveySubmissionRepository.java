package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Student;
import com.hostelsync.backend.entity.Survey;
import com.hostelsync.backend.entity.SurveySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveySubmissionRepository extends JpaRepository<SurveySubmission, Long> {

    List<SurveySubmission> findBySurveyOrderBySubmittedAtDesc(Survey survey);

    List<SurveySubmission> findBySurveyIdOrderBySubmittedAtDesc(Long surveyId);

    Optional<SurveySubmission> findBySurveyAndStudent(Survey survey, Student student);

    Optional<SurveySubmission> findBySurveyIdAndStudentId(Long surveyId, Long studentId);

    boolean existsBySurveyIdAndStudentId(Long surveyId, Long studentId);

    long countBySurveyId(Long surveyId);
}
