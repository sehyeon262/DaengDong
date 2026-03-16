package com.e108.be.domain.walk.dto.response;

import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
public class WalkDurationResponse {

    private Long walkId;
    private String status;
    private Integer duration;   // 초(seconds) 단위

    public static WalkDurationResponse from(WalkRecord walkRecord) {
        int duration;

        if (walkRecord.getWalkStatus() == WalkStatus.IN_PROGRESS) {
            // 진행 중: 현재 시각 - 시작 시각 (초)
            duration = (int) ChronoUnit.SECONDS.between(walkRecord.getStartTime(), LocalDateTime.now());
        } else {
            // 완료·취소: totalDuration 저장값 사용, 없으면 종료 - 시작 (초)
            if (walkRecord.getTotalDuration() != null) {
                duration = walkRecord.getTotalDuration();
            } else if (walkRecord.getEndTime() != null) {
                duration = (int) ChronoUnit.SECONDS.between(walkRecord.getStartTime(), walkRecord.getEndTime());
            } else {
                duration = 0;
            }
        }

        return WalkDurationResponse.builder()
                .walkId(walkRecord.getId())
                .status(walkRecord.getWalkStatus().name())
                .duration(duration)
                .build();
    }
}
