package com.e108.be.domain.route.util;

/**
 * 지리 좌표 계산 유틸리티
 *
 * - haversine: 두 좌표 간 직선 거리 (미터)
 * - bearing: 기준점에서 대상점까지 방위각 (0~360도)
 * - estimateRadius: 산책 시간 → 검색 반경 변환
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000.0;
    private static final double WALK_SPEED_M_PER_MIN = 67.0; // 평균 도보 속도 (~4km/h)

    private GeoUtils() {
    }

    /**
     * Haversine 공식으로 두 좌표 사이 직선 거리 계산
     *
     * @return 거리 (미터)
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * 기준점에서 대상점까지의 방위각 (bearing)
     *
     * @return 0 ~ 360도 (북쪽 기준 시계 방향)
     */
    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(Math.toRadians(lat2));
        double x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                - Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }

    /**
     * 목표 산책 시간(분)으로부터 검색 반경(미터) 추정
     *
     * 왕복 원형 루프를 가정하여 총 이동 거리의 1/3을 반경으로 사용
     */
    public static double estimateRadius(int targetMinutes) {
        double totalDistance = WALK_SPEED_M_PER_MIN * targetMinutes;
        return totalDistance / 3.0;
    }
}
