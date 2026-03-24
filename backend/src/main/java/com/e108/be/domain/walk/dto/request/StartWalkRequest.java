package com.e108.be.domain.walk.dto.request;

import com.e108.be.domain.route.dto.response.RouteType;
import com.e108.be.domain.route.entity.WeatherCondition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 산책 시작 요청
 *
 * 경로 추천 기반 산책 시작 시, 선택한 경로 정보를 함께 전달한다.
 * 자유 산책(경로 미선택)인 경우 route 관련 필드는 null로 전달한다.
 */
@Getter
@NoArgsConstructor
public class StartWalkRequest {

    private Long dogId;

    // ── 경로 선택 정보 (선택, 자유 산책 시 null) ──

    /**
     * 선택한 경로 유형 (SHORT / RECOMMENDED / EXPLORE / WALK_ONLY)
     * null이면 자유 산책으로 간주
     */
    private RouteType selectedType;

    /**
     * 선택한 경로의 총 거리 (미터)
     */
    @Min(0)
    private Integer selectedDistanceM;

    /**
     * 선택한 경로에 포함된 장소 ID 목록 (방문 순서)
     */
    @Size(max = 20)
    private List<Long> placeIds;

    /**
     * 현재 날씨
     */
    private WeatherCondition weatherCondition;

    /**
     * 현재 기온 (°C)
     */
    private Double temperature;

    /**
     * 추천 경로의 실제 도로 좌표 (TMap actualPathPoints)
     * 산책 종료 시 이탈률 계산에 활용
     * [[lat, lon], [lat, lon], ...]
     */
    private List<List<Double>> recommendedPath;

    /**
     * 경로 추천 기반 산책인지 여부
     */
    public boolean hasRouteSelection() {
        return selectedType != null;
    }
}
