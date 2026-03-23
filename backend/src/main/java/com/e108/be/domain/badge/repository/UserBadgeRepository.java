package com.e108.be.domain.badge.repository;

import com.e108.be.domain.badge.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findAllByUserId(Long userId);

    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);
}
