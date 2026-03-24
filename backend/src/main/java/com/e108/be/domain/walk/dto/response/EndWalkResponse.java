package com.e108.be.domain.walk.dto.response;

import com.e108.be.domain.badge.dto.response.BadgeResponse;
import com.e108.be.domain.walk.entity.WalkRecord;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class EndWalkResponse {

    private Long walkId;
    private LocalDateTime endTime;
    private Integer totalDuration;   // 초(seconds) 단위
    private Double totalDistance;    // km 단위
    private Double calories;
    private Double deviationRate;   // 경로 이탈률 (%, 추천 경로 산책인 경우만)
    private List<BadgeResponse> newBadges;  // 새로 획득한 배지

    public static EndWalkResponse from(WalkRecord walkRecord) {
        return EndWalkResponse.builder()
                .walkId(walkRecord.getId())
                .endTime(walkRecord.getEndTime())
                .totalDuration(walkRecord.getTotalDuration())
                .totalDistance(walkRecord.getTotalDistance() != null
                        ? walkRecord.getTotalDistance().doubleValue() : 0.0)
                .calories(walkRecord.getCalories() != null
                        ? walkRecord.getCalories().doubleValue() : 0.0)
                .deviationRate(walkRecord.getDeviationRate() != null
                        ? walkRecord.getDeviationRate().doubleValue() : null)
                .build();
    }

    public static EndWalkResponse from(WalkRecord walkRecord, List<BadgeResponse> newBadges) {
        return EndWalkResponse.builder()
                .walkId(walkRecord.getId())
                .endTime(walkRecord.getEndTime())
                .totalDuration(walkRecord.getTotalDuration())
                .totalDistance(walkRecord.getTotalDistance() != null
                        ? walkRecord.getTotalDistance().doubleValue() : 0.0)
                .calories(walkRecord.getCalories() != null
                        ? walkRecord.getCalories().doubleValue() : 0.0)
                .deviationRate(walkRecord.getDeviationRate() != null
                        ? walkRecord.getDeviationRate().doubleValue() : null)
                .newBadges(newBadges)
                .build();
    }
}
