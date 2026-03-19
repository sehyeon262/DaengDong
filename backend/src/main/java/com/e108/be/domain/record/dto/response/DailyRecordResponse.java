package com.e108.be.domain.record.dto.response;

import com.e108.be.domain.walk.entity.WalkRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DailyRecordResponse {

    private LocalDate date;
    private List<WalkItem> walks;

    @Getter
    @Builder
    public static class WalkItem {
        private Long recordId;
        private LocalDateTime startedAt;
        private int durationMinutes;
        private double distanceKm;
        private String thumbnailUrl;

        public static WalkItem from(WalkRecord record) {
            String thumbnail = null;
            if (record.getPhotoUrls() != null && !record.getPhotoUrls().isEmpty()) {
                thumbnail = record.getPhotoUrls().get(0);
            }

            return WalkItem.builder()
                    .recordId(record.getId())
                    .startedAt(record.getStartTime())
                    .durationMinutes(record.getTotalDuration() != null ? record.getTotalDuration() / 60 : 0)
                    .distanceKm(record.getTotalDistance() != null
                            ? record.getTotalDistance().doubleValue() / 1000.0 : 0.0)
                    .thumbnailUrl(thumbnail)
                    .build();
        }
    }
}
