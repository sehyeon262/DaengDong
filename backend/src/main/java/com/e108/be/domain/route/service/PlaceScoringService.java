package com.e108.be.domain.route.service;

import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import com.e108.be.domain.route.entity.UserCategoryPreference;
import com.e108.be.domain.route.repository.UserCategoryPreferenceRepository;
import com.e108.be.domain.route.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 장소 스코어링
 *
 * 각 장소에 대해 거리, 카테고리 가중치, 개인화 선호도, 피로도를 종합하여
 * 단일 점수를 산출한다.
 *
 * score = WEIGHT_DISTANCE × 거리 점수
 *       + WEIGHT_CATEGORY × 카테고리 점수 (전역 + 개인화 블렌딩)
 *       - RECENT_VISIT_PENALTY (최근 방문 장소 감점)
 *       + noise
 */
@Service
@RequiredArgsConstructor
public class PlaceScoringService {

    private static final double WEIGHT_DISTANCE = 0.5;
    private static final double WEIGHT_CATEGORY = 0.5;

    // 카테고리 routeWeight 정규화 기준값 (place_category 테이블 최대값 기준)
    private static final double MAX_ROUTE_WEIGHT = 10.0;

    // 개인화 블렌딩: 전역 가중치 vs 개인 선호도 비율
    private static final double GLOBAL_WEIGHT_RATIO = 0.6;
    private static final double PERSONAL_WEIGHT_RATIO = 0.4;

    // 최근 방문 장소 감점 (피로도/다양성 관리)
    // 최근 방문 장소는 이 값만큼 스코어가 낮아져 다른 장소가 상위로 올라옴
    private static final double RECENT_VISIT_PENALTY = 0.3;

    // 랜덤 노이즈 범위 (매 요청마다 약간의 변화 부여)
    private static final double NOISE_RANGE = 0.1;

    // 개인화 적용 최소 선택 횟수
    private static final int MIN_SELECTIONS_FOR_PERSONALIZATION = 5;

    private final UserCategoryPreferenceRepository preferenceRepository;

    /**
     * 장소 하나에 대한 추천 점수 계산
     *
     * @param place          주변 장소 Projection
     * @param originLat      사용자 현재 위도
     * @param originLon      사용자 현재 경도
     * @param prefMap        사용자 카테고리 선호도 맵 (categoryName -> preferenceScore)
     *                       null이면 전역 가중치만 사용
     * @param recentPlaceIds 최근 방문한 장소 ID 집합 (피로도 감점 대상)
     *                       null이면 감점 없음
     * @return 0 이상의 점수 (높을수록 추천도 높음)
     */
    public double score(NearbyPlaceProjection place, double originLat, double originLon,
                        Map<String, Double> prefMap, Set<Long> recentPlaceIds) {
        double score = 0.0;

        // 1. 거리 점수 (가까울수록 높음)
        double distanceM = place.getDistanceMeters() != null
                ? place.getDistanceMeters()
                : GeoUtils.haversine(originLat, originLon, place.getLatitude(), place.getLongitude());
        score += WEIGHT_DISTANCE * (1.0 / (1.0 + distanceM / 1000.0));

        // 2. 카테고리 점수 (전역 가중치 + 개인화 블렌딩)
        int routeWeight = place.getRouteWeight() != null ? place.getRouteWeight() : 0;
        double globalCategoryScore = routeWeight / MAX_ROUTE_WEIGHT;

        if (prefMap != null && !prefMap.isEmpty() && place.getCategoryName() != null) {
            double personalScore = prefMap.getOrDefault(place.getCategoryName(), 0.0);
            double blendedScore = GLOBAL_WEIGHT_RATIO * globalCategoryScore
                    + PERSONAL_WEIGHT_RATIO * personalScore;
            score += WEIGHT_CATEGORY * blendedScore;
        } else {
            score += WEIGHT_CATEGORY * globalCategoryScore;
        }

        // 3. 피로도 감점 (최근 방문 장소는 점수 하락 → 다른 장소 우선 추천)
        if (recentPlaceIds != null && recentPlaceIds.contains(place.getId())) {
            score -= RECENT_VISIT_PENALTY;
        }

        // 4. 랜덤 노이즈 (같은 출발지에서 매번 같은 결과 방지)
        score += ThreadLocalRandom.current().nextDouble(-NOISE_RANGE, NOISE_RANGE);

        return Math.max(0, score);
    }

    /**
     * 사용자의 카테고리 선호도 맵 조회
     *
     * @param memberId 회원 ID
     * @return categoryName -> preferenceScore 맵, 데이터 부족 시 null
     */
    public Map<String, Double> loadPreferenceMap(Long memberId) {
        if (memberId == null) return null;

        List<UserCategoryPreference> prefs = preferenceRepository.findByMemberId(memberId);

        // 선택 이력이 부족하면 개인화 미적용
        int totalSelections = prefs.stream()
                .mapToInt(UserCategoryPreference::getSelectionCount)
                .sum();
        if (totalSelections < MIN_SELECTIONS_FOR_PERSONALIZATION) {
            return null;
        }

        return prefs.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.toMap(
                        p -> p.getCategory().getName(),
                        UserCategoryPreference::getPreferenceScore,
                        (a, b) -> a // 중복 시 첫 번째 값 유지
                ));
    }
}
