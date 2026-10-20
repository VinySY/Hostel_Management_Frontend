package com.hostelsync.backend.repository;

import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.Warden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WardenRepository extends JpaRepository<Warden, Long> {

    Optional<Warden> findByUser(User user);

    Optional<Warden> findByUserId(Long userId);
}
