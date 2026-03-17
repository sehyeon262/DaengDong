package com.e108.be.domain.walk.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /walks/start 요청 바디
 * 산책 시작 시 dog_id를 받아 세션 생성
 */
@Getter
@NoArgsConstructor
public class WalkStartRequest {

    private Long dogId;
}
