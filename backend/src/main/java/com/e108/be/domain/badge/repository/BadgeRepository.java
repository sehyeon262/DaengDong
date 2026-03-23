package com.e108.be.domain.badge.repository;

import com.e108.be.domain.badge.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findByBadgeName(String badgeName);
}
