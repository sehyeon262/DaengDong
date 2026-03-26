package com.e108.be.domain.safety.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 주변 위험 구역 조회 응답 DTO (거리 정보 포함)
 *
 * GET /api/v1/safety/risk-zones/nearby 응답용
 * 산책 중 근접 알림에 사용
 */
@Getter
@Builder
public class NearbyRiskReportResponse {

    private Long riskReportId;
    private Long walkSessionId;
    private Double latitude;
    private Double longitude;
    private String description;
    private LocalDateTime createdAt;

    /**
     * 현재 위치에서 위험 구역까지의 거리 (미터 단위)
     */
    private Double distanceM;
}
