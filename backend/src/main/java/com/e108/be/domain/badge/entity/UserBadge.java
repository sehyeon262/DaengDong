package com.e108.be.domain.badge.entity;

import com.e108.be.domain.auth.entity.User;
import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_badges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBadge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Builder
    public UserBadge(User user, Badge badge) {
        this.user = user;
        this.badge = badge;
    }
}
