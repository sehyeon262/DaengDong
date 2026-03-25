package com.e108.be.domain.route.service;

import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import com.e108.be.domain.place.repository.PlaceRepository;
import com.e108.be.domain.route.dto.response.FallbackLevel;
import com.e108.be.domain.route.dto.response.RouteDetailResponse;
import com.e108.be.domain.route.dto.response.RouteRecommendResponse;
import com.e108.be.domain.route.dto.response.RouteType;
import com.e108.be.domain.route.entity.WeatherCondition;
import com.e108.be.domain.route.repository.BreedDistanceConfigRepository;
import com.e108.be.domain.route.repository.RouteSelectionPlaceRepository;
import com.e108.be.domain.route.service.RouteGeneratorService.ScoredPlace;
import com.e108.be.domain.route.service.WalkPatternService.WalkPattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 경로 추천 오케스트레이션 서비스
 *
 * 경로별 검색 반경을 차등 적용하여 거리감이 다른 3개 경로를 생성한다.
 * 사용자의 산책 패턴, 날씨 선호도, 반려견 체중을 반영하여 개인화된 추천을 제공한다.
 *
 * - 빠른 산책:  500m 반경 → 가까운 장소 위주 짧은 코스
 * - 추천 코스:  1000m 반경 → 균형 잡힌 중간 코스
 * - 탐험 코스:  1500m 반경 → 넓은 범위 긴 코스
 *
 * 각 반경에서 장소가 부족하면 자동 확장 (최대 x2, 3km 제한)
 * 전체적으로 장소가 부족하면 폴백 적용
 */
@Slf4j
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

    private static final int CANDIDATE_LIMIT = 20;

    // 폴백 기준: 전체 장소 수
    private static final int NORMAL_MIN = 6;
    private static final int REDUCED_MIN = 3;

    // 피로도 감점 적용 기간 (일)
    private static final int RECENT_VISIT_DAYS = 3;

    private final PlaceRepository placeRepository;
    private final DogRepository dogRepository;
    private final PlaceScoringService placeScoringService;
    private final RouteGeneratorService routeGeneratorService;
    private final WalkPatternService walkPatternService;
    private final BreedDistanceConfigRepository breedDistanceConfigRepository;
    private final RouteSelectionPlaceRepository selectionPlaceRepository;

    /**
     * 경로 추천
     *
     * 로그인한 사용자의 반려견과 산책 패턴을 분석하여
     * 개인화된 경로를 추천한다.
     *
     * @param userId 회원 ID (JWT에서 추출)
     * @param lat      기준 위도
     * @param lon      기준 경도
     * @param weather  현재 날씨 (nullable)
     * @return 폴백 레벨에 따른 경로 응답
     */
    public RouteRecommendResponse recommend(Long userId, double lat, double lon,
                                             WeatherCondition weather) {
        Dog dog = dogRepository.findFirstByUser_Id(userId).orElse(null);

        // 개인화 컨텍스트 구성 (개인 선호도 → 세그먼트 CF → 전역 가중치 순으로 fallback)
        BigDecimal dogWeight = dog != null ? dog.getWeight() : null;
        Map<String, Double> prefMap = placeScoringService.loadPreferenceMap(userId, dogWeight);
        double radiusMultiplier = resolveRadiusMultiplier(dog);
        Set<Long> recentPlaceIds = loadRecentPlaceIds(userId);

        // 학습된 스코어링 가중치 1회 로드 (N+1 방지)
        Map<String, Double> weights = placeScoringService.loadGlobalWeights();

        // 산책 패턴 분석 (날씨/시간 기반 추천에 활용)
        RouteType recommendedType = resolveRecommendedType(userId, dog, weather);

        // 반경 조정 (체중 기반)
        double shortRadius = SHORT_RADIUS_M * radiusMultiplier;
        double recommendRadius = RECOMMEND_RADIUS_M * radiusMultiplier;
        double exploreRadius = EXPLORE_RADIUS_M * radiusMultiplier;

        log.debug("경로 추천 - userId={}, 반경 배율={}, short={}m, recommend={}m, explore={}m, 추천유형={}, 최근방문={}개",
                userId, radiusMultiplier, shortRadius, recommendRadius, exploreRadius,
                recommendedType, recentPlaceIds != null ? recentPlaceIds.size() : 0);

        // 1단계: 반경별 후보 장소 조회 + 개인화 스코어링 + 피로도 감점
        List<ScoredPlace> shortPlaces = findAndScore(lat, lon, shortRadius, prefMap, recentPlaceIds, weights);
        List<ScoredPlace> recommendPlaces = findAndScore(lat, lon, recommendRadius, prefMap, recentPlaceIds, weights);
        List<ScoredPlace> explorePlaces = findAndScore(lat, lon, exploreRadius, prefMap, recentPlaceIds, weights);

        // 전체 고유 장소 수로 폴백 레벨 결정
        int totalUnique = countUniquePlaces(shortPlaces, recommendPlaces, explorePlaces);

        if (totalUnique >= NORMAL_MIN) {
            return buildNormalResponse(lat, lon, shortPlaces, recommendPlaces, explorePlaces, recommendedType);
        } else if (totalUnique >= REDUCED_MIN) {
            return buildReducedResponse(lat, lon, shortPlaces, recommendPlaces, recommendedType);
        } else {
            return buildWalkOnlyResponse(lat, lon, shortPlaces);
        }
    }

    /**
     * 날씨/시간/패턴 기반 추천 경로 유형 결정
     *
     * 우선순위:
     * 1. 날씨별 선호 유형 (사용자 이력 기반)
     * 2. 전체 선호 유형 (가장 많이 선택한 유형)
     * 3. null (데이터 부족 시 추천 없음)
     */
    private RouteType resolveRecommendedType(Long userId, Dog dog, WeatherCondition weather) {
        if (userId == null) return null;

        Long dogId = dog != null ? dog.getId() : null;

        try {
            WalkPattern pattern = walkPatternService.analyze(dogId, userId);

            if (!pattern.hasEnoughData()) {
                return null;
            }

            // 1순위: 날씨별 선호 유형
            if (weather != null && pattern.weatherPreferences() != null) {
                RouteType weatherPref = pattern.weatherPreferences().get(weather);
                if (weatherPref != null) {
                    log.debug("날씨 기반 추천 유형: weather={}, type={}", weather, weatherPref);
                    return weatherPref;
                }
            }

            // 2순위: 전체 선호 유형
            log.debug("패턴 기반 추천 유형: type={}", pattern.preferredType());
            return pattern.preferredType();

        } catch (Exception e) {
            log.warn("산책 패턴 분석 실패, 기본 추천 사용: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 반려견 체중 기반 반경 배율 계산
     *
     * 소형견: 권장 거리가 짧으므로 반경 축소 (0.7배)
     * 대형견: 권장 거리가 길므로 반경 확대 (1.3배)
     * 중형견/설정 없음: 기본 (1.0배)
     */
    private double resolveRadiusMultiplier(Dog dog) {
        if (dog == null || dog.getWeight() == null) {
            return 1.0;
        }

        return breedDistanceConfigRepository.findByWeight(dog.getWeight())
                .map(config -> {
                    // 권장 거리 기준으로 배율 산출 (1000m 기준)
                    double ratio = config.getRecommendedDistanceM() / 1000.0;
                    // 0.7 ~ 1.5 범위로 제한
                    return Math.max(0.7, Math.min(1.5, ratio));
                })
                .orElse(1.0);
    }

    /**
     * 최근 N일 내 방문 장소 ID 조회 (피로도 감점용)
     */
    private Set<Long> loadRecentPlaceIds(Long userId) {
        if (userId == null) return Set.of();

        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_VISIT_DAYS);
        return selectionPlaceRepository.findRecentPlaceIds(userId, since);
    }

    /**
     * 지정 반경에서 후보 장소를 조회하고 개인화 스코어링
     * 장소 부족 시 반경을 단계별로 확장 (최대 x2, 3km 제한)
     */
    private List<ScoredPlace> findAndScore(double lat, double lon, double baseRadius,
                                            Map<String, Double> prefMap,
                                            Set<Long> recentPlaceIds,
                                            Map<String, Double> weights) {
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
                        placeScoringService.score(place, lat, lon, prefMap, recentPlaceIds, weights)
                ))
                .sorted(Comparator.comparingDouble(ScoredPlace::score).reversed())
                .toList();
    }

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

    private RouteRecommendResponse buildNormalResponse(
            double lat, double lon,
            List<ScoredPlace> shortPlaces,
            List<ScoredPlace> recommendPlaces,
            List<ScoredPlace> explorePlaces,
            RouteType recommendedType) {

        List<RouteDetailResponse> routes = routeGeneratorService.generateRoutes(
                lat, lon, shortPlaces, recommendPlaces, explorePlaces);

        // 추천 유형이 있으면 해당 경로를 목록 맨 앞으로 이동
        if (recommendedType != null) {
            routes = reorderByPreferredType(routes, recommendedType);
        }

        return RouteRecommendResponse.of(FallbackLevel.NORMAL, null, recommendedType, routes);
    }

    private RouteRecommendResponse buildReducedResponse(
            double lat, double lon,
            List<ScoredPlace> shortPlaces,
            List<ScoredPlace> recommendPlaces,
            RouteType recommendedType) {

        List<RouteDetailResponse> routes = routeGeneratorService.generateReducedRoutes(
                lat, lon, shortPlaces, recommendPlaces);

        if (recommendedType != null) {
            routes = reorderByPreferredType(routes, recommendedType);
        }

        return RouteRecommendResponse.of(
                FallbackLevel.REDUCED,
                "주변 장소가 적어 2개 코스를 추천합니다.",
                recommendedType,
                routes
        );
    }

    /**
     * 추천 경로 유형을 목록 맨 앞으로 이동
     * 해당 유형이 목록에 없으면 원래 순서 유지
     */
    private List<RouteDetailResponse> reorderByPreferredType(
            List<RouteDetailResponse> routes, RouteType preferredType) {

        List<RouteDetailResponse> reordered = new ArrayList<>(routes.size());
        List<RouteDetailResponse> rest = new ArrayList<>();

        for (RouteDetailResponse route : routes) {
            if (route.getType() == preferredType) {
                reordered.add(route);
            } else {
                rest.add(route);
            }
        }

        if (reordered.isEmpty()) {
            return routes; // 해당 유형이 없으면 원래 순서
        }

        reordered.addAll(rest);
        return reordered;
    }

    private RouteRecommendResponse buildWalkOnlyResponse(
            double lat, double lon,
            List<ScoredPlace> shortPlaces) {

        List<RouteDetailResponse> routes = routeGeneratorService.generateWalkOnlyRoutes(
                lat, lon, shortPlaces);
        return RouteRecommendResponse.of(
                FallbackLevel.WALK_ONLY,
                "주변에 등록된 장소가 적어 산책 경로 위주로 추천합니다.",
                routes
        );
    }
}
