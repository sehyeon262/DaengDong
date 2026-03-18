package com.e108.be.domain.record.dto.response;

import com.e108.be.domain.walk.entity.WalkRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class RecordDetailResponse {

    private Long recordId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private int durationMinutes;
    private double distanceKm;
    private double calories;
    private List<String> photoUrls;

    public static RecordDetailResponse from(WalkRecord record) {
        return RecordDetailResponse.builder()
                .recordId(record.getId())
                .startedAt(record.getStartTime())
                .endedAt(record.getEndTime())
                .durationMinutes(record.getTotalDuration() != null ? record.getTotalDuration() / 60 : 0)
                .distanceKm(record.getTotalDistance() != null
                        ? record.getTotalDistance().doubleValue() / 1000.0 : 0.0)
                .calories(record.getCalories() != null ? record.getCalories().doubleValue() : 0.0)
                .photoUrls(record.getPhotoUrls() != null ? record.getPhotoUrls() : Collections.emptyList())
                .build();
    }
}
