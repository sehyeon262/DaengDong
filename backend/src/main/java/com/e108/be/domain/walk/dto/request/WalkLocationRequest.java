package com.e108.be.domain.walk.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * POST /walks/{walkId}/locations 요청 바디
 *
 * FE에서 주기적으로 (예: 5초마다) GPS 좌표 배치를 전송
 *
 * 요청 예시:
 * {
 *   "locations": [
 *     {"latitude": 37.5665, "longitude": 126.9780, "timestamp": 1234567890000},
 *     {"latitude": 37.5670, "longitude": 126.9785, "timestamp": 1234567895000}
 *   ]
 * }
 */
@Getter
@NoArgsConstructor
public class WalkLocationRequest {

    private List<LocationPoint> locations;

    @Getter
    @NoArgsConstructor
    public static class LocationPoint {
        private double latitude;   // 위도
        private double longitude;  // 경도
        private long timestamp;    // 수집 시각 (Unix ms)
    }
}
