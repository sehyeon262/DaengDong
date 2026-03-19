package com.e108.be.domain.route.service;

import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import com.e108.be.domain.place.repository.PlaceRepository;
import com.e108.be.domain.route.dto.response.RouteDetailResponse;
import com.e108.be.domain.route.dto.response.RouteRecommendResponse;
import com.e108.be.domain.route.service.RouteGeneratorService.ScoredPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 경로 추천 오케스트레이션 서비스
 *
 * 경로별 검색 반경을 차등 적용하여 거리감이 다른 3개 경로를 생성한다.
 *
 * - 빠른 산책:  500m 반경 → 가까운 장소 위주 짧은 코스
 * - 추천 코스:  1000m 반경 → 균형 잡힌 중간 코스
 * - 탐험 코스:  1500m 반경 → 넓은 범위 긴 코스
 *
 * 각 반경에서 장소가 부족하면 자동 확장 (최대 ×2, 3km 제한)
 * 전체적으로 장소가 부족하면 폴백 적용
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    // 경로별 기본 검색 반경 (미터)
    private static final double SHORT_RADIUS_M = 500.0;
    private static final double RECOMMEND_RADIUS_M = 1000.0;
    private static final double EXPLORE_RADIUS_M = 1500.0;

    // 반경 확장 배율 (부족 시 단계별 확장)
    private static final double[] RADIUS_MULTIPLIERS = {1.0, 1.5, 2.0};
    private static final double MAX_RADIUS_M = 3000.0;

    private static final int CANDIDATE_LIMIT = 20; // 반경별 최대 후보 수

    // 폴백 기준: 전체 장소 수
    private static final int NORMAL_MIN = 6;   // 3개 경로 가능
    private static final int REDUCED_MIN = 3;  // 2개 경로 가능

    private final PlaceRepository placeRepository;
    private final DogRepository dogRepository;
    private final PlaceScoringService placeScoringService;
    private final RouteGeneratorService routeGeneratorService;

    /**
     * 경로 추천
     *
     * 로그인한 사용자의 첫 번째 반려견을 자동 조회하여 경로를 추천한다.
     * TODO: 반려견 정보를 스코어링에 반영 (견종별 적정 거리, 활동 수준 등)
     *
     * @param memberId 회원 ID (JWT에서 추출)
     * @param lat      기준 위도
     * @param lon      기준 경도
     * @return 폴백 레벨에 따른 경로 응답
     */
    public RouteRecommendResponse recommend(Long memberId, double lat, double lon) {
        // 사용자의 반려견 자동 조회 (추후 스코어링에 활용)
        Dog dog = dogRepository.findFirstByUser_Id(memberId).orElse(null);

        // 1단계: 반경별 후보 장소 조회
        List<ScoredPlace> shortPlaces = findAndScore(lat, lon, SHORT_RADIUS_M);
        List<ScoredPlace> recommendPlaces = findAndScore(lat, lon, RECOMMEND_RADIUS_M);
        List<ScoredPlace> explorePlaces = findAndScore(lat, lon, EXPLORE_RADIUS_M);

        // 전체 고유 장소 수로 폴백 레벨 결정
        int totalUnique = countUniquePlaces(shortPlaces, recommendPlaces, explorePlaces);

        if (totalUnique >= NORMAL_MIN) {
            return buildNormalResponse(lat, lon, shortPlaces, recommendPlaces, explorePlaces);
        } else if (totalUnique >= REDUCED_MIN) {
            return buildReducedResponse(lat, lon, shortPlaces, recommendPlaces);
        } else {
            return buildWalkOnlyResponse(lat, lon, shortPlaces);
        }
    }

    /**
     * 지정 반경에서 후보 장소를 조회하고 스코어링
     * 장소 부족 시 반경을 단계별로 확장 (최대 ×2, 3km 제한)
     */
    private List<ScoredPlace> findAndScore(double lat, double lon, double baseRadius) {
        List<NearbyPlaceProjection> candidates = List.of();

        for (double multiplier : RADIUS_MULTIPLIERS) {
            double radius = Math.min(baseRadius * multiplier, MAX_RADIUS_M);
            candidates = placeRepository.findNearby(lat, lon, radius, CANDIDATE_LIMIT);

            if (candidates.size() >= 3) {
                break;
            }
        }

        return candidates.stream()
                .map(place -> new ScoredPlace(
                        place,
                        placeScoringService.score(place, lat, lon)
                ))
                .sorted(Comparator.comparingDouble(ScoredPlace::score).reversed())
                .toList();
    }

    /**
     * 여러 리스트에서 고유 장소 ID 수 계산
     */
    private int countUniquePlaces(
            List<ScoredPlace> list1,
            List<ScoredPlace> list2,
            List<ScoredPlace> list3) {
        Set<Long> ids = new HashSet<>();
        list1.forEach(sp -> ids.add(sp.place().getId()));
        list2.forEach(sp -> ids.add(sp.place().getId()));
        list3.forEach(sp -> ids.add(sp.place().getId()));
        return ids.size();
    }

    /**
     * NORMAL: 3개 경로 (빠른 산책 500m, 추천 1km, 탐험 1.5km)
     */
    private RouteRecommendResponse buildNormalResponse(
            double lat, double lon,
            List<ScoredPlace> shortPlaces,
            List<ScoredPlace> recommendPlaces,
            List<ScoredPlace> explorePlaces) {

        List<RouteDetailResponse> routes = routeGeneratorService.generateRoutes(
                lat, lon, shortPlaces, recommendPlaces, explorePlaces);
        return RouteRecommendResponse.of("NORMAL", null, routes);
    }

    /**
     * REDUCED: 2개 경로 (빠른 산책, 추천 코스)
     */
    private RouteRecommendResponse buildReducedResponse(
            double lat, double lon,
            List<ScoredPlace> shortPlaces,
            List<ScoredPlace> recommendPlaces) {

        List<RouteDetailResponse> routes = routeGeneratorService.generateReducedRoutes(
                lat, lon, shortPlaces, recommendPlaces);
        return RouteRecommendResponse.of(
                "REDUCED",
                "주변 장소가 적어 2개 코스를 추천합니다.",
                routes
        );
    }

    /**
     * WALK_ONLY: 장소 경유 + 방향별 순수 산책 코스
     */
    private RouteRecommendResponse buildWalkOnlyResponse(
            double lat, double lon,
            List<ScoredPlace> shortPlaces) {

        List<RouteDetailResponse> routes = routeGeneratorService.generateWalkOnlyRoutes(
                lat, lon, shortPlaces);
        return RouteRecommendResponse.of(
                "WALK_ONLY",
                "주변에 등록된 장소가 적어 산책 경로 위주로 추천합니다.",
                routes
        );
    }
}
