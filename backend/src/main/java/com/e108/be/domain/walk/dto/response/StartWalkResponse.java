package com.e108.be.domain.walk.dto.response;

import com.e108.be.domain.walk.entity.WalkRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StartWalkResponse {
    private Long walkId;
    private LocalDateTime startTime;
    private String status;

    public static StartWalkResponse from(WalkRecord walkRecord) {
        return StartWalkResponse.builder()
                .walkId(walkRecord.getId())
                .startTime(walkRecord.getStartTime())
                .status(walkRecord.getWalkStatus().name())
                .build();
    }
}
