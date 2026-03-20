package com.e108.be.domain.walk.dto.response;

import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.walk.entity.Feedback;
import com.e108.be.domain.walk.entity.MetDog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MetDogResponse {

    private Long metDogId;
    private Long targetDogId;
    private String targetDogName;
    private String targetDogBreed;
    private String targetDogProfileImageUrl;
    private String feedback;
    private boolean feedbackDone;
    private LocalDateTime lastMetAt;
    private Long lastWalkRecordId;

    public static MetDogResponse from(MetDog metDog, Dog targetDog) {
        return MetDogResponse.builder()
                .metDogId(metDog.getId())
                .targetDogId(targetDog.getId())
                .targetDogName(targetDog.getName())
                .targetDogBreed(targetDog.getBreed())
                .targetDogProfileImageUrl(targetDog.getProfileImageUrl())
                .feedback(metDog.getFeedback().name())
                .feedbackDone(metDog.getFeedback() != Feedback.보통)
                .lastMetAt(metDog.getUpdatedAt())
                .lastWalkRecordId(metDog.getLatestWalkRecord().getId())
                .build();
    }
}
