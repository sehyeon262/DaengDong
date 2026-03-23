package com.e108.be.domain.badge.dto.response;

import com.e108.be.domain.badge.entity.Badge;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BadgeResponse {

    private Long badgeId;
    private String badgeName;
    private String description;
    private String conditionValue;

    public static BadgeResponse from(Badge badge) {
        return BadgeResponse.builder()
                .badgeId(badge.getId())
                .badgeName(badge.getBadgeName())
                .description(badge.getDescription())
                .conditionValue(badge.getConditionValue())
                .build();
    }
}
