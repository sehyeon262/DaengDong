package com.e108.be.domain.route.service;

import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import com.e108.be.domain.route.dto.response.RouteDetailResponse;
import com.e108.be.domain.route.dto.response.RoutePlaceResponse;
import com.e108.be.domain.route.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 경로 생성기
 *
 * 반경별로 분리된 장소 목록을 받아 거리감이 다른 경로를 생성한다.
 *
 * NORMAL:
 * - 빠른 산책 (500m 반경):  가까운 장소 2~3개 → 짧은 루프
 * - 추천 코스 (1km 반경):   스코어 상위 3~5개 → 중간 루프
 * - 탐험 코스 (1.5km 반경): 다른 경로에 없는 장소 → 넓은 루프
 *
 * REDUCED: 빠른 산책 + 추천 코스 (2개)
 * WALK_ONLY: 장소 경유 + 방향별 순수 산책
 */
@Service
@RequiredArgsConstructor
public class RouteGeneratorService {

    private static final int SHORT_PLACE_COUNT = 3;
    private static final int RECOMMEND_PLACE_COUNT = 5;
    private static final int EXPLORE_PLACE_COUNT = 4;

    // WALK_ONLY 순수 산책 코스 반경별 거리 (미터)
    private static final double[] WALK_ONLY_DISTANCES = {300.0, 500.0, 700.0};
    private static final String[] WALK_ONLY_NAMES = {"가벼운 산책", "보통 산책", "긴 산책"};

    // ==================== NORMAL (3개 경로) ====================

    /**
     * 정상: 3개 경로 생성 (반경별 차등)
     *
     * @param shortPlaces     500m 반경 스코어링 결과
     * @param recommendPlaces 1km 반경 스코어링 결과
     * @param explorePlaces   1.5km 반경 스코어링 결과
     */
    public List<RouteDetailResponse> generateRoutes(
            double originLat, double originLon,
            List<ScoredPlace> shortPlaces,
            List<ScoredPlace> recommendPlaces,
            List<ScoredPlace> explorePlaces) {

        List<RouteDetailResponse> routes = new ArrayList<>();

        // 경로 1: 빠른 산책 - 500m 반경 내 거리순 상위
        List<NearbyPlaceProjection> shortList = shortPlaces.stream()
                .sorted(Comparator.comparingDouble(sp -> sp.place().getDistanceMeters()))
                .limit(SHORT_PLACE_COUNT)
                .map(ScoredPlace::place)
                .toList();
        routes.add(buildLoop(originLat, originLon, shortList, "빠른 산책", "SHORT"));

        // 경로 2: 추천 코스 - 1km 반경 내 스코어순 상위
        List<NearbyPlaceProjection> recommendList = recommendPlaces.stream()
                .limit(RECOMMEND_PLACE_COUNT)
                .map(ScoredPlace::place)
                .toList();
        routes.add(buildLoop(originLat, originLon, recommendList, "추천 코스", "RECOMMENDED"));

        // 경로 3: 탐험 코스 - 1.5km 반경에서 다른 경로에 없는 장소 우선
        Set<Long> usedIds = new HashSet<>();
        shortList.forEach(p -> usedIds.add(p.getId()));
        recommendList.forEach(p -> usedIds.add(p.getId()));

        List<NearbyPlaceProjection> exploreList = explorePlaces.stream()
                .filter(sp -> !usedIds.contains(sp.place().getId()))
                .limit(EXPLORE_PLACE_COUNT)
                .map(ScoredPlace::place)
                .toList();

        // 필터링 후 장소가 부족하면 중복 허용하여 채움
        if (exploreList.size() < 2) {
            exploreList = explorePlaces.stream()
                    .limit(EXPLORE_PLACE_COUNT)
                    .map(ScoredPlace::place)
                    .toList();
        }
        routes.add(buildLoop(originLat, originLon, exploreList, "탐험 코스", "EXPLORE"));

        return routes;
    }

    // ==================== REDUCED (2개 경로) ====================

    /**
     * 축소: 2개 경로 생성 (빠른 산책 + 추천 코스)
     */
    public List<RouteDetailResponse> generateReducedRoutes(
            double originLat, double originLon,
            List<ScoredPlace> shortPlaces,
            List<ScoredPlace> recommendPlaces) {

        List<RouteDetailResponse> routes = new ArrayList<>();

        // 경로 1: 빠른 산책 - 가장 가까운 2개
        List<NearbyPlaceProjection> shortList = shortPlaces.stream()
                .sorted(Comparator.comparingDouble(sp -> sp.place().getDistanceMeters()))
                .limit(2)
                .map(ScoredPlace::place)
                .toList();

        // 500m에서 부족하면 1km 풀에서 가져옴
        if (shortList.size() < 2) {
            shortList = recommendPlaces.stream()
                    .sorted(Comparator.comparingDouble(sp -> sp.place().getDistanceMeters()))
                    .limit(2)
                    .map(ScoredPlace::place)
                    .toList();
        }
        routes.add(buildLoop(originLat, originLon, shortList, "빠른 산책", "SHORT"));

        // 경로 2: 추천 코스 - 전체 장소 활용
        List<NearbyPlaceProjection> allPlaces = recommendPlaces.stream()
                .map(ScoredPlace::place)
                .toList();
        routes.add(buildLoop(originLat, originLon, allPlaces, "추천 코스", "RECOMMENDED"));

        return routes;
    }

    // ==================== WALK_ONLY (장소 부족) ====================

    /**
     * 산책 위주: 있는 장소를 경유하는 원형 코스 + 거리별 순수 산책 코스
     */
    public List<RouteDetailResponse> generateWalkOnlyRoutes(
            double originLat, double originLon,
            List<ScoredPlace> scoredPlaces) {

        List<RouteDetailResponse> routes = new ArrayList<>();

        // 장소가 1개 이상이면 경유 코스 생성
        if (!scoredPlaces.isEmpty()) {
            List<NearbyPlaceProjection> places = scoredPlaces.stream()
                    .map(ScoredPlace::place)
                    .toList();
            routes.add(buildLoop(originLat, originLon, places, "장소 경유 산책", "SHORT"));
        }

        // 거리별 순수 산책 코스 (120도 간격 방향, 거리 차등)
        int walkRouteCount = 3 - routes.size();
        double[] directions = {0.0, 120.0, 240.0};

        for (int i = 0; i < walkRouteCount; i++) {
            routes.add(buildDirectionalWalk(
                    originLat, originLon,
                    directions[i], WALK_ONLY_DISTANCES[i], WALK_ONLY_NAMES[i]
            ));
        }

        return routes;
    }

    // ==================== 공통 빌더 ====================

    /**
     * 장소들을 방위각 순서로 정렬하여 원형 루프 경로 구성
     *
     * 출발지 → 장소1 → 장소2 → ... → 출발지
     */
    private RouteDetailResponse buildLoop(
            double originLat, double originLon,
            List<NearbyPlaceProjection> places,
            String name, String type) {

        // 방위각(bearing) 기준 정렬 → 원형 동선
        List<NearbyPlaceProjection> sorted = new ArrayList<>(places);
        sorted.sort(Comparator.comparingDouble(p ->
                GeoUtils.bearing(originLat, originLon, p.getLatitude(), p.getLongitude())
        ));

        // polyline 좌표: 출발지 → 각 장소 → 출발지
        List<double[]> polyline = new ArrayList<>();
        polyline.add(new double[]{originLat, originLon});
        for (NearbyPlaceProjection p : sorted) {
            polyline.add(new double[]{p.getLatitude(), p.getLongitude()});
        }
        polyline.add(new double[]{originLat, originLon});

        int totalDistanceM = calculateTotalDistance(polyline);
        int estimatedMinutes = (int) Math.ceil(totalDistanceM / 67.0); // 4km/h 기준

        List<RoutePlaceResponse> placeResponses = sorted.stream()
                .map(RoutePlaceResponse::from)
                .toList();

        return RouteDetailResponse.builder()
                .name(name)
                .type(type)
                .totalDistanceM(totalDistanceM)
                .estimatedMinutes(estimatedMinutes)
                .places(placeResponses)
                .polyline(polyline)
                .build();
    }

    /**
     * 방향 + 거리 기반 순수 산책 코스 생성
     *
     * 출발지 → 지정 방향/거리 지점 → 출발지 (왕복)
     * TODO: Kakao 도보 길찾기 API 연동 시 실제 도보 경로로 교체
     */
    private RouteDetailResponse buildDirectionalWalk(
            double originLat, double originLon,
            double bearingDeg, double distanceM, String name) {

        double[] destination = GeoUtils.destinationPoint(originLat, originLon, bearingDeg, distanceM);

        List<double[]> polyline = List.of(
                new double[]{originLat, originLon},
                destination,
                new double[]{originLat, originLon}
        );

        int totalDistanceM = (int) (distanceM * 2);
        int estimatedMinutes = (int) Math.ceil(totalDistanceM / 67.0);

        return RouteDetailResponse.builder()
                .name(name)
                .type("WALK_ONLY")
                .totalDistanceM(totalDistanceM)
                .estimatedMinutes(estimatedMinutes)
                .places(List.of())
                .polyline(polyline)
                .build();
    }

    /**
     * polyline 좌표 배열의 총 직선 거리 합산 (미터)
     */
    private int calculateTotalDistance(List<double[]> polyline) {
        double total = 0;
        for (int i = 0; i < polyline.size() - 1; i++) {
            total += GeoUtils.haversine(
                    polyline.get(i)[0], polyline.get(i)[1],
                    polyline.get(i + 1)[0], polyline.get(i + 1)[1]
            );
        }
        return (int) Math.round(total);
    }

    /**
     * 스코어링된 장소 레코드
     */
    public record ScoredPlace(NearbyPlaceProjection place, double score) {
    }
}
