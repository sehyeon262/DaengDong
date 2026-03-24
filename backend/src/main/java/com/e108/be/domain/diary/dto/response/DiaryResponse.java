package com.e108.be.domain.diary.dto.response;

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
public class DiaryResponse {

    private Long diaryId;
    private Long walkId;
    private String content;
    private String emotionTag;
    private Map<String, String> photoEmotions;
    private String dogName;
    private String dogProfileImageUrl;
    private LocalDateTime walkDate;
    private int durationMinutes;
    private double distanceKm;
    private double calories;
    private List<String> photoUrls;
    private LocalDateTime createdAt;

    public static DiaryResponse from(Diary diary, WalkRecord walk, Dog dog) {
        return DiaryResponse.builder()
                .diaryId(diary.getId())
                .walkId(diary.getWalkId())
                .content(diary.getContent())
                .emotionTag(diary.getEmotionTag())
                .photoEmotions(diary.getPhotoEmotions())
                .dogName(dog.getName())
                .dogProfileImageUrl(dog.getProfileImageUrl())
                .walkDate(walk.getStartTime())
                .durationMinutes(walk.getTotalDuration() != null ? walk.getTotalDuration() / 60 : 0)
                .distanceKm(walk.getTotalDistance() != null
                        ? walk.getTotalDistance().doubleValue() / 1000.0 : 0.0)
                .calories(walk.getCalories() != null ? walk.getCalories().doubleValue() : 0.0)
                .photoUrls(walk.getPhotoUrls() != null ? walk.getPhotoUrls() : Collections.emptyList())
                .createdAt(diary.getCreatedAt())
                .build();
    }
}
