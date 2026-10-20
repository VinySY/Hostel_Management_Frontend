package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Complaint;
import com.hostelsync.backend.entity.ComplaintTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintTimelineRepository extends JpaRepository<ComplaintTimeline, Long> {

    List<ComplaintTimeline> findByComplaintOrderByTimestampAsc(Complaint complaint);
}
