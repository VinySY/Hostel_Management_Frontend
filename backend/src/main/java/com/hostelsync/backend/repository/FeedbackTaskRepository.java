package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.FeedbackTask;
import com.hostelsync.backend.entity.FeedbackTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackTaskRepository extends JpaRepository<FeedbackTask, Long> {

    List<FeedbackTask> findByWardenIdOrderByCreatedAtDesc(Long wardenId);

    List<FeedbackTask> findByHostelIdOrderByCreatedAtDesc(Long hostelId);

    List<FeedbackTask> findByStatusOrderByCreatedAtDesc(FeedbackTaskStatus status);

    List<FeedbackTask> findAllByOrderByCreatedAtDesc();

    long countByWardenIdAndStatus(Long wardenId, FeedbackTaskStatus status);
}
