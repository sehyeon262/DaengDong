package com.e108.be.domain.safety.dto.response;

import com.e108.be.domain.badge.dto.response.BadgeResponse;
import com.e108.be.domain.safety.entity.RiskReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RiskReportResponse {

    private Long riskReportId;
    private Long walkSessionId;
    private Double latitude;
    private Double longitude;
    private String description;
    private LocalDateTime createdAt;
    private List<BadgeResponse> newBadges;

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

    public static RiskReportResponse from(RiskReport riskReport, List<BadgeResponse> newBadges) {
        return RiskReportResponse.builder()
                .riskReportId(riskReport.getId())
                .walkSessionId(riskReport.getWalkSessionId())
                .latitude(riskReport.getLocation().getY())
                .longitude(riskReport.getLocation().getX())
                .description(riskReport.getDescription())
                .createdAt(riskReport.getCreatedAt())
                .newBadges(newBadges)
                .build();
    }
}
