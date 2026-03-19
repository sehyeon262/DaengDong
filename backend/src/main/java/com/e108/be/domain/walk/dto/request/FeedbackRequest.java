package com.e108.be.domain.walk.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FeedbackRequest {
    private Long targetDogId;
    private Long myWalkRecordId;
    private String feedback;  // "좋아요" | "보통" | "싫어요"
}
