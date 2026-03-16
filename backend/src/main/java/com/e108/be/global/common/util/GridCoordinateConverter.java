package com.e108.be.global.common.util;

/**
 * 위경도 → 기상청 격자좌표(nx, ny) 변환 유틸
 *
 * 기상청 API는 위경도가 아닌 격자좌표를 사용한다.
 * Lambert Conformal Conic 투영법 기반 변환 공식.
 *
 * 사용 예시:
 *   int[] grid = GridCoordinateConverter.convert(35.1796, 129.0756);
 *   int nx = grid[0]; // 98
 *   int ny = grid[1]; // 76
 */
public class GridCoordinateConverter {

    private static final double RE = 6371.00877;    // 지구 반경 (km)
    private static final double GRID = 5.0;          // 격자 간격 (km)
    private static final double SLAT1 = 30.0;        // 투영 위도1 (degree)
    private static final double SLAT2 = 60.0;        // 투영 위도2 (degree)
    private static final double OLON = 126.0;        // 기준점 경도 (degree)
    private static final double OLAT = 38.0;         // 기준점 위도 (degree)
    private static final double XO = 43;             // 기준점 X좌표 (GRID)
    private static final double YO = 136;            // 기준점 Y좌표 (GRID)

    private GridCoordinateConverter() {
    }

    /**
     * 위경도 → 격자좌표 변환
     *
     * @param latitude  위도
     * @param longitude 경도
     * @return int[]{nx, ny} 격자 X, Y 좌표
     */
    public static int[] convert(double latitude, double longitude) {
        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + latitude * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = longitude * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new int[]{nx, ny};
    }
}
