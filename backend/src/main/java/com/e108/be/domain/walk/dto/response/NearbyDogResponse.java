package com.e108.be.domain.walk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NearbyDogResponse {
    private Long dogId;
    private String name;
    private String breed;
    private String profileImageUrl;
    private double latitude;
    private double longitude;
    private double distanceM;
    private Long walkRecordId;
    /** 내 강아지가 이 강아지에게 설정한 피드백 (null = 만난 적 없음 or 보통, "싫어요" = 비선호) */
    private String feedback;
}
