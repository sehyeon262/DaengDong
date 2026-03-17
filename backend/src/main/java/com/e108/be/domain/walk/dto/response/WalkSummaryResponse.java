package com.e108.be.domain.walk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 산책 요약 응답 (거리 + 칼로리 통합)
 */
@Getter
@AllArgsConstructor
public class WalkSummaryResponse {

    private Long walkId;
    private BigDecimal totalDistance; // 단위: m
    private BigDecimal calories;      // 단위: kcal
    private Integer totalDuration;    // 단위: 초
}
