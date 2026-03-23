package com.e108.be.domain.dog.dto.response;

import com.e108.be.domain.badge.dto.response.BadgeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RegisterDogResponse {
    private Long dogId;
    private String name;
    private List<BadgeResponse> newBadges;
}
