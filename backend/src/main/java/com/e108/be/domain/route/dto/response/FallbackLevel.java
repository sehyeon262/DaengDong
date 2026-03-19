package com.e108.be.domain.route.dto.response;

/**
 * 경로 추천 폴백 레벨
 */
public enum FallbackLevel {
    NORMAL,    // 정상: 3개 경로
    REDUCED,   // 축소: 2개 경로 (장소 부족)
    WALK_ONLY  // 산책 위주: 장소 거의 없음
}
