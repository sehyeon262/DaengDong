package com.e108.be.domain.route.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    private List<LatLng> polyline;  // preview 성격의 직선 좌표 (기존 호환)

    /**
     * 실제 도로 기반 경로 좌표 (TMAP 등 길찾기 API 결과)
     * null이면 polyline을 fallback으로 사용
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<LatLng> actualPathPoints;

    /**
     * 경로 제공자 (예: "tmap", "preview")
     * actualPathPoints가 null이면 "preview"로 간주
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String provider;

    /**
     * 실제 도로 기반 여부
     * true: 길찾기 API로 계산된 실제 도로 경로
     * false/null: 직선 연결 (preview)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean roadBased;
}
