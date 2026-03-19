package com.e108.be.domain.walk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NearbyDogsResponse {
    private List<NearbyDogResponse> nearbyDogs;
    // 172번 (산책 제안) 작업 시 채워질 필드. 현재는 빈 리스트 반환.
    private List<Object> pendingProposals;
}
