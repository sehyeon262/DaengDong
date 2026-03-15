package com.e108.be.domain.walk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * GET /walks/{walkId}/distance 응답
 *
 * 응답 예시:
 * {
 *   "distanceM": 1234.56,
 *   "distanceKm": 1.23
 * }
 */
@Getter
@AllArgsConstructor
public class DistanceResponse {
    private double distanceM;   // 미터 단위
    private double distanceKm;  // km 단위 (소수점 2자리)
}
