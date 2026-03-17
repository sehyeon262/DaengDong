package com.e108.be.domain.walk.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /walks/{walkId}/end 요청 바디
 * 산책 종료 시 walkId를 받아 세션 완료 처리
 */
@Getter
@NoArgsConstructor
public class WalkEndRequest {

    private Long walkId;
}
