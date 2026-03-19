package com.e108.be.domain.route.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RouteRecommendResponse {

    private List<RouteDetailResponse> routes;

    public static RouteRecommendResponse from(List<RouteDetailResponse> routes) {
        return RouteRecommendResponse.builder()
                .routes(routes)
                .build();
    }
}
