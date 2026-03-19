package com.e108.be.domain.safety.dto.response;

import com.e108.be.domain.safety.entity.RiskReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RiskReportResponse {

    private Long riskReportId;
    private Long walkSessionId;
    private Double latitude;
    private Double longitude;
    private String description;
    private LocalDateTime createdAt;

    /**
     * RiskReport 엔티티 → RiskReportResponse 변환
     *
     * PostGIS Point 좌표 추출:
     *   location.getX() → 경도 (longitude)
     *   location.getY() → 위도 (latitude)
     */
    public static RiskReportResponse from(RiskReport riskReport) {
        return RiskReportResponse.builder()
                .riskReportId(riskReport.getId())
                .walkSessionId(riskReport.getWalkSessionId())
                .latitude(riskReport.getLocation().getY())
                .longitude(riskReport.getLocation().getX())
                .description(riskReport.getDescription())
                .createdAt(riskReport.getCreatedAt())
                .build();
    }
}
