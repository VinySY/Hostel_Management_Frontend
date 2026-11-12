package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Dgm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DgmRepository extends JpaRepository<Dgm, Long> {
    Optional<Dgm> findByUserId(Long userId);
}
