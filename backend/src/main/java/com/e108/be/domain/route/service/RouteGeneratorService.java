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
 * 스코어링된 장소 목록을 받아 3가지 성격의 원형 루프 경로를 생성한다.
 * - 최단 코스 (SHORT): 가까운 장소 2~3개
 * - 추천 코스 (RECOMMENDED): 스코어 상위 장소 3~5개
 * - 탐험 코스 (EXPLORE): 추천 코스에 없는 장소 중 스코어 상위
 */
@Service
@RequiredArgsConstructor
public class RouteGeneratorService {

    private static final int SHORT_PLACE_COUNT = 3;
    private static final int RECOMMEND_PLACE_COUNT = 5;
    private static final int EXPLORE_PLACE_COUNT = 4;

    /**
     * 3개 경로 생성
     *
     * @param originLat    출발 위도
     * @param originLon    출발 경도
     * @param scoredPlaces 스코어 내림차순 정렬된 (장소, 점수) 목록
     * @return 3개 RouteDetailResponse 리스트
     */
    public List<RouteDetailResponse> generateRoutes(
            double originLat, double originLon,
            List<ScoredPlace> scoredPlaces) {

        List<RouteDetailResponse> routes = new ArrayList<>();

        // 경로 1: 최단 코스 - 거리순 상위
        List<NearbyPlaceProjection> shortList = scoredPlaces.stream()
                .sorted(Comparator.comparingDouble(sp -> sp.place().getDistanceMeters()))
                .limit(SHORT_PLACE_COUNT)
                .map(ScoredPlace::place)
                .toList();
        routes.add(buildLoop(originLat, originLon, shortList, "빠른 산책", "SHORT"));

        // 경로 2: 추천 코스 - 스코어순 상위
        List<NearbyPlaceProjection> recommendList = scoredPlaces.stream()
                .limit(RECOMMEND_PLACE_COUNT)
                .map(ScoredPlace::place)
                .toList();
        routes.add(buildLoop(originLat, originLon, recommendList, "추천 코스", "RECOMMENDED"));

        // 경로 3: 탐험 코스 - 추천 코스 장소 제외 후 스코어순 상위
        Set<Long> usedIds = recommendList.stream()
                .map(NearbyPlaceProjection::getId)
                .collect(Collectors.toSet());
        List<NearbyPlaceProjection> exploreList = scoredPlaces.stream()
                .filter(sp -> !usedIds.contains(sp.place().getId()))
                .limit(EXPLORE_PLACE_COUNT)
                .map(ScoredPlace::place)
                .toList();
        routes.add(buildLoop(originLat, originLon, exploreList, "새로운 코스", "EXPLORE"));

        return routes;
    }

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

        // 총 직선 거리 계산
        int totalDistanceM = calculateTotalDistance(polyline);

        // 예상 시간 (분) - 평균 도보 속도 4km/h 기준
        int estimatedMinutes = (int) Math.ceil(totalDistanceM / 67.0);

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
