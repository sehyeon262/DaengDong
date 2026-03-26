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

    /**
     * 사용자 산책 패턴/날씨 기반으로 추천하는 경로 유형 힌트
     * 프론트에서 해당 경로를 강조 표시하는 데 사용
     * null이면 별도 추천 없음 (기본 순서 그대로)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RouteType recommendedRouteType;

    private List<RouteDetailResponse> routes;

    public static RouteRecommendResponse of(
            FallbackLevel fallbackLevel, String fallbackMessage, List<RouteDetailResponse> routes) {
        return RouteRecommendResponse.builder()
                .fallbackLevel(fallbackLevel)
                .fallbackMessage(fallbackMessage)
                .routes(routes)
                .build();
    }

    public static RouteRecommendResponse of(
            FallbackLevel fallbackLevel, String fallbackMessage,
            RouteType recommendedRouteType, List<RouteDetailResponse> routes) {
        return RouteRecommendResponse.builder()
                .fallbackLevel(fallbackLevel)
                .fallbackMessage(fallbackMessage)
                .recommendedRouteType(recommendedRouteType)
                .routes(routes)
                .build();
    }
}
