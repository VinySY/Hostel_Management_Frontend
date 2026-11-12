package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.SlaConfiguration;
import com.hostelsync.backend.entity.ComplaintPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SlaConfigurationRepository extends JpaRepository<SlaConfiguration, Long> {
    Optional<SlaConfiguration> findByPriority(ComplaintPriority priority);
}
