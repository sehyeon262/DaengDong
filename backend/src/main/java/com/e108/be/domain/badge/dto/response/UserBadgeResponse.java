package com.e108.be.domain.badge.dto.response;

import com.e108.be.domain.badge.entity.UserBadge;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserBadgeResponse {

    private Long badgeId;
    private String badgeName;
    private String description;
    private LocalDateTime earnedAt;

    public static UserBadgeResponse from(UserBadge userBadge) {
        return UserBadgeResponse.builder()
                .badgeId(userBadge.getBadge().getId())
                .badgeName(userBadge.getBadge().getBadgeName())
                .description(userBadge.getBadge().getDescription())
                .earnedAt(userBadge.getCreatedAt())
                .build();
    }
}
