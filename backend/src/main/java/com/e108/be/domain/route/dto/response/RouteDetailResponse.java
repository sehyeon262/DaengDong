package com.e108.be.domain.route.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RouteDetailResponse {

    private String name;
    private RouteType type;
    private int totalDistanceM;     // 총 거리 (미터)
    private int estimatedMinutes;   // 예상 소요 시간 (분)
    private List<RoutePlaceResponse> places;
    private List<LatLng> polyline;
}
