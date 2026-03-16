package com.e108.be.domain.walk.dto.response;

import com.e108.be.domain.walk.entity.WalkRecord;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class EndWalkResponse {

    private Long walkId;
    private LocalDateTime endTime;
    private Integer totalDuration;   // 초(seconds) 단위
    private Double totalDistance;    // km 단위
    private Double calories;

    public static EndWalkResponse from(WalkRecord walkRecord) {
        return EndWalkResponse.builder()
                .walkId(walkRecord.getId())
                .endTime(walkRecord.getEndTime())
                .totalDuration(walkRecord.getTotalDuration())
                .totalDistance(walkRecord.getTotalDistance() != null
                        ? walkRecord.getTotalDistance().doubleValue() : 0.0)
                .calories(walkRecord.getCalories() != null
                        ? walkRecord.getCalories().doubleValue() : 0.0)
                .build();
    }
}
