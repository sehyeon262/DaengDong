package com.e108.be.domain.route.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RouteRecommendResponse {

    private FallbackLevel fallbackLevel;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String fallbackMessage; // 폴백 시 안내 메시지 (정상이면 null)

    private List<RouteDetailResponse> routes;

    public static RouteRecommendResponse of(
            FallbackLevel fallbackLevel, String fallbackMessage, List<RouteDetailResponse> routes) {
        return RouteRecommendResponse.builder()
                .fallbackLevel(fallbackLevel)
                .fallbackMessage(fallbackMessage)
                .routes(routes)
                .build();
    }
}
