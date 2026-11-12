package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    List<Survey> findByActiveTrueOrderByCreatedAtDesc();

    @Query("""
           SELECT s FROM Survey s
           WHERE s.active = true
           AND (s.targetHostel IS NULL OR s.targetHostel.id = :hostelId)
           ORDER BY s.createdAt DESC
           """)
    List<Survey> findActiveSurveysForHostel(@Param("hostelId") Long hostelId);

    List<Survey> findAllByOrderByCreatedAtDesc();
}
