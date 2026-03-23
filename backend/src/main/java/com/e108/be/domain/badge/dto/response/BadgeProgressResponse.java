package com.e108.be.domain.badge.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BadgeProgressResponse {

    private Long badgeId;
    private String badgeName;
    private String description;
    private int currentValue;
    private int targetValue;
    private boolean earned;
}
