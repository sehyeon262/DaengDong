package com.e108.be.domain.route.service;

import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import com.e108.be.domain.route.dto.response.*;
import com.e108.be.domain.route.service.TmapPathService.PathResult;
import com.e108.be.domain.route.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 경로 생성기
 *
 * 반경별로 분리된 장소 목록을 받아 거리감이 다른 경로를 생성한다.
 * 장소 방문 순서는 Nearest Neighbor 알고리즘으로 결정하여
 * 자연스러운 산책 동선을 구성한다.
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
@Slf4j
public class RouteGeneratorService {

    private final TmapPathService tmapPathService;

    private static final int SHORT_PLACE_COUNT = 3;
    private static final int RECOMMEND_PLACE_COUNT = 5;
    private static final int EXPLORE_PLACE_COUNT = 4;

    // WALK_ONLY 순수 산책 코스 반경별 거리 (미터)
    private static final double[] WALK_ONLY_DISTANCES = {300.0, 500.0, 700.0};
    private static final String PREVIEW_ROUTE_NAME = "가벼운 산책 코스";
    private static final long PREVIEW_TRAIL_ID = -900001L;
    private static final long PREVIEW_PARK_ID = -900002L;
    private static final long PREVIEW_RESTROOM_ID = -900003L;
    private static final LatLng PREVIEW_FALLBACK_START = new LatLng(35.09205, 128.85295);
    private static final LatLng PREVIEW_TRAIL_POINT = new LatLng(35.092346, 128.85295);
    private static final LatLng PREVIEW_PARK_POINT = new LatLng(35.09309167, 128.853142);
    private static final LatLng PREVIEW_RESTROOM_POINT = new LatLng(35.093079, 128.853172);
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
        routes.add(buildLoop(originLat, originLon, shortList, "빠른 산책", RouteType.SHORT));

        // 경로 2: 추천 코스 - 1km 반경 내 스코어순 상위
        List<NearbyPlaceProjection> recommendList = recommendPlaces.stream()
                .limit(RECOMMEND_PLACE_COUNT)
                .map(ScoredPlace::place)
                .toList();
        routes.add(buildLoop(originLat, originLon, recommendList, "추천 코스", RouteType.RECOMMENDED));

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
        routes.add(buildLoop(originLat, originLon, exploreList, "탐험 코스", RouteType.EXPLORE));

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
        routes.add(buildLoop(originLat, originLon, shortList, "빠른 산책", RouteType.SHORT));

        // 경로 2: 추천 코스 - 전체 장소 활용
        List<NearbyPlaceProjection> allPlaces = recommendPlaces.stream()
                .map(ScoredPlace::place)
                .toList();
        routes.add(buildLoop(originLat, originLon, allPlaces, "추천 코스", RouteType.RECOMMENDED));

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
            routes.add(buildLoop(originLat, originLon, places, "장소 경유 산책", RouteType.SHORT));
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
     * Nearest Neighbor 알고리즘으로 방문 순서를 결정하여 원형 루프 경로 구성
     *
     * 출발지에서 가장 가까운 장소를 먼저 방문하고,
     * 현재 위치에서 가장 가까운 미방문 장소를 순차적으로 방문한 뒤 출발지로 복귀한다.
     *
     * 출발지 → (가장 가까운) 장소1 → 장소2 → ... → 출발지
     *
     * TMAP API를 통해 실제 도로 기반 경로를 조회하고,
     * 실패 시 기존 직선 경로를 fallback으로 사용한다.
     */
    private RouteDetailResponse buildLoop(
            double originLat, double originLon,
            List<NearbyPlaceProjection> places,
            String name, RouteType type) {

        // Nearest Neighbor로 방문 순서 결정
        List<NearbyPlaceProjection> ordered = nearestNeighborOrder(originLat, originLon, places);

        // preview polyline 좌표: 출발지 → 각 장소 → 출발지 (직선 연결)
        List<LatLng> polyline = new ArrayList<>();
        polyline.add(new LatLng(originLat, originLon));
        for (NearbyPlaceProjection p : ordered) {
            polyline.add(new LatLng(p.getLatitude(), p.getLongitude()));
        }
        polyline.add(new LatLng(originLat, originLon));

        // 기본값: Haversine 직선 거리
        int totalDistanceM = calculateTotalDistance(polyline);
        int estimatedMinutes = (int) Math.ceil(totalDistanceM / 67.0); // 4km/h 기준

        List<RoutePlaceResponse> placeResponses = ordered.stream()
                .map(RoutePlaceResponse::from)
                .toList();

        // TMAP으로 실제 도보 경로 조회 시도
        LatLng origin = new LatLng(originLat, originLon);
        PathResult pathResult = tmapPathService.getWalkingPath(origin, placeResponses);

        if (pathResult != null) {
            // TMAP 성공: 실제 도로 기반 경로 사용
            log.debug("TMAP 경로 적용: {} - {}m, {}분", name, pathResult.totalDistanceM(), pathResult.estimatedMinutes());
            return RouteDetailResponse.builder()
                    .name(name)
                    .type(type)
                    .totalDistanceM(pathResult.totalDistanceM())
                    .estimatedMinutes(pathResult.estimatedMinutes())
                    .places(placeResponses)
                    .polyline(polyline) // 기존 직선 경로 (preview용 유지)
                    .actualPathPoints(pathResult.pathPoints())
                    .provider(pathResult.provider())
                    .roadBased(pathResult.roadBased())
                    .build();
        }

        // TMAP 실패: 기존 직선 경로 사용
        log.debug("직선 경로 사용 (TMAP 실패 또는 비활성화): {}", name);
        return RouteDetailResponse.builder()
                .name(name)
                .type(type)
                .totalDistanceM(totalDistanceM)
                .estimatedMinutes(estimatedMinutes)
                .places(placeResponses)
                .polyline(polyline)
                .provider("preview")
                .roadBased(false)
                .build();
    }

    /**
     * Nearest Neighbor 알고리즘
     *
     * 현재 위치에서 가장 가까운 미방문 장소를 반복적으로 선택하여
     * 자연스러운 산책 동선을 구성한다. (장소 3~5개 수준에서 최적에 근접)
     */
    private List<NearbyPlaceProjection> nearestNeighborOrder(
            double startLat, double startLon,
            List<NearbyPlaceProjection> places) {

        if (places.size() <= 1) {
            return new ArrayList<>(places);
        }

        List<NearbyPlaceProjection> remaining = new ArrayList<>(places);
        List<NearbyPlaceProjection> ordered = new ArrayList<>();

        double currentLat = startLat;
        double currentLon = startLon;

        while (!remaining.isEmpty()) {
            // 현재 위치에서 가장 가까운 장소 찾기
            int nearestIdx = 0;
            double nearestDist = Double.MAX_VALUE;

            for (int i = 0; i < remaining.size(); i++) {
                NearbyPlaceProjection p = remaining.get(i);
                double dist = GeoUtils.haversine(currentLat, currentLon, p.getLatitude(), p.getLongitude());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestIdx = i;
                }
            }

            NearbyPlaceProjection nearest = remaining.remove(nearestIdx);
            ordered.add(nearest);
            currentLat = nearest.getLatitude();
            currentLon = nearest.getLongitude();
        }

        return ordered;
    }

    /**
     * 방향 + 거리 기반 순수 산책 코스 생성
     *
     * 출발지 → 지정 방향/거리 지점 → 출발지 (왕복)
     * TMAP API로 실제 도보 경로를 조회하고, 실패 시 직선 경로 사용
     */
    private RouteDetailResponse buildDirectionalWalk(
            double originLat, double originLon,
            double bearingDeg, double distanceM, String name) {

        double[] destination = GeoUtils.destinationPoint(originLat, originLon, bearingDeg, distanceM);

        // preview polyline (직선)
        List<LatLng> polyline = List.of(
                new LatLng(originLat, originLon),
                new LatLng(destination[0], destination[1]),
                new LatLng(originLat, originLon)
        );

        int totalDistanceM = (int) (distanceM * 2);
        int estimatedMinutes = (int) Math.ceil(totalDistanceM / 67.0);

        // TMAP으로 실제 도보 경로 조회 시도
        LatLng origin = new LatLng(originLat, originLon);
        LatLng dest = new LatLng(destination[0], destination[1]);
        PathResult pathResult = tmapPathService.getDirectionalWalkingPath(origin, dest);

        if (pathResult != null) {
            log.debug("TMAP 순수 산책 경로 적용: {} - {}m, {}분", name, pathResult.totalDistanceM(), pathResult.estimatedMinutes());
            return RouteDetailResponse.builder()
                    .name(name)
                    .type(RouteType.WALK_ONLY)
                    .totalDistanceM(pathResult.totalDistanceM())
                    .estimatedMinutes(pathResult.estimatedMinutes())
                    .places(List.of())
                    .polyline(polyline)
                    .actualPathPoints(pathResult.pathPoints())
                    .provider(pathResult.provider())
                    .roadBased(pathResult.roadBased())
                    .build();
        }

        log.debug("직선 경로 사용 (순수 산책): {}", name);
        return RouteDetailResponse.builder()
                .name(name)
                .type(RouteType.WALK_ONLY)
                .totalDistanceM(totalDistanceM)
                .estimatedMinutes(estimatedMinutes)
                .places(List.of())
                .polyline(polyline)
                .provider("preview")
                .roadBased(false)
                .build();
    }

    /**
     * polyline 좌표의 총 직선 거리 합산 (미터)
     */
    public RouteDetailResponse buildPreviewRoute(double originLat, double originLon) {
        List<RoutePlaceResponse> previewPlaces = List.of(
                buildPreviewPlace(PREVIEW_TRAIL_ID, "산책로", PREVIEW_TRAIL_POINT, originLat, originLon, null),
                buildPreviewPlace(PREVIEW_PARK_ID, "공원", PREVIEW_PARK_POINT, originLat, originLon, "부산광역시 강서구 녹산산단321로 50"),
                buildPreviewPlace(PREVIEW_RESTROOM_ID, "화장실", PREVIEW_RESTROOM_POINT, originLat, originLon, null)
        );

        List<LatLng> fallbackPolyline = List.of(
                PREVIEW_FALLBACK_START,
                PREVIEW_TRAIL_POINT,
                PREVIEW_PARK_POINT,
                PREVIEW_RESTROOM_POINT
        );

        PathResult pathResult = tmapPathService.getWalkingPath(
                new LatLng(originLat, originLon),
                previewPlaces.subList(0, previewPlaces.size() - 1),
                PREVIEW_RESTROOM_POINT
        );

        if (pathResult != null) {
            return RouteDetailResponse.builder()
                    .name(PREVIEW_ROUTE_NAME)
                    .type(RouteType.RECOMMENDED)
                    .totalDistanceM(pathResult.totalDistanceM())
                    .estimatedMinutes(pathResult.estimatedMinutes())
                    .places(previewPlaces)
                    .polyline(fallbackPolyline)
                    .actualPathPoints(pathResult.pathPoints())
                    .provider(pathResult.provider())
                    .roadBased(pathResult.roadBased())
                    .build();
        }

        int fallbackDistanceM = calculateTotalDistance(fallbackPolyline);
        int fallbackMinutes = Math.max(1, (int) Math.ceil(fallbackDistanceM / 67.0));
        return RouteDetailResponse.builder()
                .name(PREVIEW_ROUTE_NAME)
                .type(RouteType.RECOMMENDED)
                .totalDistanceM(fallbackDistanceM)
                .estimatedMinutes(fallbackMinutes)
                .places(previewPlaces)
                .polyline(fallbackPolyline)
                .provider("preview")
                .roadBased(false)
                .build();
    }

    private RoutePlaceResponse buildPreviewPlace(
            long id,
            String name,
            LatLng point,
            double originLat,
            double originLon,
            String address
    ) {
        return RoutePlaceResponse.builder()
                .id(id)
                .name(name)
                .categoryName(name)
                .latitude(point.latitude())
                .longitude(point.longitude())
                .distanceMeters(GeoUtils.haversine(originLat, originLon, point.latitude(), point.longitude()))
                .address(address)
                .imageUrl(null)
                .build();
    }

    private int calculateTotalDistance(List<LatLng> polyline) {
        double total = 0;
        for (int i = 0; i < polyline.size() - 1; i++) {
            total += GeoUtils.haversine(
                    polyline.get(i).latitude(), polyline.get(i).longitude(),
                    polyline.get(i + 1).latitude(), polyline.get(i + 1).longitude()
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
