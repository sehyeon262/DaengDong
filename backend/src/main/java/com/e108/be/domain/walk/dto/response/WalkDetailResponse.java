package com.e108.be.domain.walk.dto.response;

import com.e108.be.domain.diary.entity.Diary;
import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.walk.entity.WalkRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class WalkDetailResponse {

    // 산책 정보
    private Long walkId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationMinutes;
    private int durationSeconds;   // 총 산책 시간(초) — 1분 미만 산책도 표시 가능
    private double distanceKm;
    private double calories;
    private List<String> photoUrls;

    // 강아지 정보
    private String dogName;
    private String dogProfileImageUrl;

    // 일기 (없으면 null)
    private DiaryInfo diary;

    @Getter
    @Builder
    public static class DiaryInfo {
        private Long diaryId;
        private String content;
        private String emotionTag;
        private Map<String, String> photoEmotions;  // { "사진URL": "즐거움" }
        private LocalDateTime createdAt;
    }

    public static WalkDetailResponse from(WalkRecord walk, Dog dog, Diary diary) {
        DiaryInfo diaryInfo = null;
        if (diary != null) {
            diaryInfo = DiaryInfo.builder()
                    .diaryId(diary.getId())
                    .content(diary.getContent())
                    .emotionTag(diary.getEmotionTag())
                    .photoEmotions(diary.getPhotoEmotions())
                    .createdAt(diary.getCreatedAt())
                    .build();
        }

        return WalkDetailResponse.builder()
                .walkId(walk.getId())
                .startTime(walk.getStartTime())
                .endTime(walk.getEndTime())
                .durationMinutes(walk.getTotalDuration() != null ? walk.getTotalDuration() / 60 : 0)
                .durationSeconds(walk.getTotalDuration() != null ? walk.getTotalDuration() : 0)
                .distanceKm(walk.getTotalDistance() != null
                        ? walk.getTotalDistance().doubleValue() / 1000.0 : 0.0)
                .calories(walk.getCalories() != null ? walk.getCalories().doubleValue() : 0.0)
                .photoUrls(walk.getPhotoUrls() != null ? walk.getPhotoUrls() : Collections.emptyList())
                .dogName(dog.getName())
                .dogProfileImageUrl(dog.getProfileImageUrl())
                .diary(diaryInfo)
                .build();
    }
}
