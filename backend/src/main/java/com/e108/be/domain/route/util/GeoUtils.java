package com.e108.be.domain.route.util;

/**
 * 지리 좌표 계산 유틸리티
 *
 * - haversine: 두 좌표 간 직선 거리 (미터)
 * - bearing: 기준점에서 대상점까지 방위각 (0~360도)
 * - destinationPoint: 기준점에서 특정 방향/거리만큼 이동한 좌표
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000.0;

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
     * 기준점에서 특정 방위각/거리만큼 이동한 도착점 좌표 계산
     *
     * @param lat        기준 위도
     * @param lon        기준 경도
     * @param bearingDeg 방위각 (0~360도, 북쪽 기준 시계 방향)
     * @param distanceM  이동 거리 (미터)
     * @return [위도, 경도] 배열
     */
    public static double[] destinationPoint(double lat, double lon, double bearingDeg, double distanceM) {
        double angularDist = distanceM / EARTH_RADIUS_M;
        double bearingRad = Math.toRadians(bearingDeg);
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);

        double destLatRad = Math.asin(
                Math.sin(latRad) * Math.cos(angularDist)
                + Math.cos(latRad) * Math.sin(angularDist) * Math.cos(bearingRad)
        );
        double destLonRad = lonRad + Math.atan2(
                Math.sin(bearingRad) * Math.sin(angularDist) * Math.cos(latRad),
                Math.cos(angularDist) - Math.sin(latRad) * Math.sin(destLatRad)
        );

        return new double[]{Math.toDegrees(destLatRad), Math.toDegrees(destLonRad)};
    }
}
