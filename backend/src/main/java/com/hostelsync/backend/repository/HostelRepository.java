package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.Hostel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HostelRepository extends JpaRepository<Hostel, Long> {

    Optional<Hostel> findByName(String name);

    boolean existsByName(String name);

    List<Hostel> findByActiveTrue();
}

