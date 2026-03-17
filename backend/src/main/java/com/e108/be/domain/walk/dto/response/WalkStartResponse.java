package com.e108.be.domain.walk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 산책 시작 응답
 * 생성된 walkId를 FE에 반환 → 이후 GPS 전송 등 모든 요청에 사용
 */
@Getter
@AllArgsConstructor
public class WalkStartResponse {

    private Long walkId;
    private String status;
}
