package com.e108.be.domain.route.service;

import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import com.e108.be.domain.route.entity.SegmentPreference;
import com.e108.be.domain.route.entity.UserCategoryPreference;
import com.e108.be.domain.route.repository.ScoringWeightRepository;
import com.e108.be.domain.route.repository.SegmentPreferenceRepository;
import com.e108.be.domain.route.repository.UserCategoryPreferenceRepository;
import com.e108.be.domain.route.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 장소 스코어링
 *
 * 각 장소에 대해 거리, 카테고리 가중치, 개인화 선호도, 피로도를 종합하여
 * 단일 점수를 산출한다.
 *
 * score = W_DISTANCE × 거리 점수
 *       + W_CATEGORY × 카테고리 점수 (전역 + 개인화/세그먼트 블렌딩)
 *       - RECENT_VISIT_PENALTY (최근 방문 장소 감점)
 *       + noise
 *
 * W_DISTANCE, W_CATEGORY는 ScoringWeightLearner가 학습한 값을 사용하며,
 * 학습 데이터 부족 시 기본값(0.5, 0.5)을 사용한다.
 *
 * 개인화 선호도는 다음 우선순위로 적용:
 * 1순위: 개인 선호도 (UserCategoryPreference, 선택 5회 이상)
 * 2순위: 세그먼트 선호도 (SegmentPreference, 같은 체중 구간 사용자 집계)
 * 3순위: 전역 카테고리 가중치만 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceScoringService {

    // 카테고리 routeWeight 정규화 기준값 (place_category 테이블 최대값 기준)
    private static final double MAX_ROUTE_WEIGHT = 10.0;

    // 개인화 블렌딩: 전역 가중치 vs 개인 선호도 비율
    private static final double GLOBAL_WEIGHT_RATIO = 0.6;
    private static final double PERSONAL_WEIGHT_RATIO = 0.4;

    // 최근 방문 장소 감점 (피로도/다양성 관리)
    private static final double RECENT_VISIT_PENALTY = 0.3;

    // 랜덤 노이즈 범위 (매 요청마다 약간의 변화 부여)
    private static final double NOISE_RANGE = 0.1;

    // 개인화 적용 최소 선택 횟수
    private static final int MIN_SELECTIONS_FOR_PERSONALIZATION = 5;

    private final UserCategoryPreferenceRepository preferenceRepository;
    private final ScoringWeightRepository scoringWeightRepository;
    private final SegmentPreferenceRepository segmentPreferenceRepository;

    /**
     * 장소 하나에 대한 추천 점수 계산
     *
     * @param place          주변 장소 Projection
     * @param originLat      사용자 현재 위도
     * @param originLon      사용자 현재 경도
     * @param prefMap        사용자 카테고리 선호도 맵 (categoryName -> preferenceScore)
     *                       null이면 세그먼트 선호도 또는 전역 가중치만 사용
     * @param recentPlaceIds 최근 방문한 장소 ID 집합 (피로도 감점 대상)
     *                       null이면 감점 없음
     * @return 0 이상의 점수 (높을수록 추천도 높음)
     */
    public double score(NearbyPlaceProjection place, double originLat, double originLon,
                        Map<String, Double> prefMap, Set<Long> recentPlaceIds) {

        // 학습된 가중치 로드 (DB에 없으면 기본값)
        double wDistance = loadWeight("DISTANCE", ScoringWeightLearner.DEFAULT_WEIGHT_DISTANCE);
        double wCategory = loadWeight("CATEGORY", ScoringWeightLearner.DEFAULT_WEIGHT_CATEGORY);

        double score = 0.0;

        // 1. 거리 점수 (가까울수록 높음)
        double distanceM = place.getDistanceMeters() != null
                ? place.getDistanceMeters()
                : GeoUtils.haversine(originLat, originLon, place.getLatitude(), place.getLongitude());
        score += wDistance * (1.0 / (1.0 + distanceM / 1000.0));

        // 2. 카테고리 점수 (전역 가중치 + 개인화/세그먼트 블렌딩)
        int routeWeight = place.getRouteWeight() != null ? place.getRouteWeight() : 0;
        double globalCategoryScore = routeWeight / MAX_ROUTE_WEIGHT;

        if (prefMap != null && !prefMap.isEmpty() && place.getCategoryName() != null) {
            double personalScore = prefMap.getOrDefault(place.getCategoryName(), 0.0);
            double blendedScore = GLOBAL_WEIGHT_RATIO * globalCategoryScore
                    + PERSONAL_WEIGHT_RATIO * personalScore;
            score += wCategory * blendedScore;
        } else {
            score += wCategory * globalCategoryScore;
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
     * 우선순위:
     * 1. 개인 선호도 (선택 5회 이상)
     * 2. 세그먼트 선호도 (같은 체중 구간, 콜드스타트 대응)
     * 3. null (전역 가중치만 사용)
     *
     * @param memberId   회원 ID
     * @param dogWeight  반려견 체중 (세그먼트 판별용, nullable)
     * @return categoryName -> preferenceScore 맵
     */
    public Map<String, Double> loadPreferenceMap(Long memberId, BigDecimal dogWeight) {
        if (memberId == null) return null;

        // 1순위: 개인 선호도
        Map<String, Double> personalMap = loadPersonalPreferenceMap(memberId);
        if (personalMap != null) {
            return personalMap;
        }

        // 2순위: 세그먼트 선호도 (콜드스타트 대응)
        return loadSegmentPreferenceMap(dogWeight);
    }

    /**
     * 개인 카테고리 선호도 조회
     */
    private Map<String, Double> loadPersonalPreferenceMap(Long memberId) {
        List<UserCategoryPreference> prefs = preferenceRepository.findByMemberId(memberId);

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
                        (a, b) -> a
                ));
    }

    /**
     * 세그먼트(체중 구간) 선호도 조회
     *
     * 같은 체중 구간의 다른 사용자들이 많이 선택한 카테고리를
     * 선호도 맵으로 반환한다. (Collaborative Filtering)
     *
     * @param dogWeight 반려견 체중 (nullable)
     * @return categoryName -> preferenceScore 맵, 데이터 없으면 null
     */
    private Map<String, Double> loadSegmentPreferenceMap(BigDecimal dogWeight) {
        if (dogWeight == null) return null;

        String weightGroup = resolveWeightGroup(dogWeight);
        List<SegmentPreference> segmentPrefs = segmentPreferenceRepository
                .findByWeightGroup(weightGroup);

        if (segmentPrefs.isEmpty()) {
            return null;
        }

        log.debug("세그먼트 CF 적용: weightGroup={}, 카테고리={}개",
                weightGroup, segmentPrefs.size());

        return segmentPrefs.stream()
                .filter(sp -> sp.getCategory() != null)
                .collect(Collectors.toMap(
                        sp -> sp.getCategory().getName(),
                        SegmentPreference::getPreferenceScore,
                        (a, b) -> a
                ));
    }

    /**
     * 체중 → 세그먼트 그룹 변환
     */
    private String resolveWeightGroup(BigDecimal weight) {
        double w = weight.doubleValue();
        if (w < 10.0) return "SMALL";
        if (w < 25.0) return "MEDIUM";
        return "LARGE";
    }

    /**
     * DB에서 학습된 가중치를 로드, 없으면 기본값 반환
     */
    private double loadWeight(String featureName, double defaultValue) {
        return scoringWeightRepository.findByFeatureName(featureName)
                .map(sw -> {
                    // 학습 데이터가 충분한 경우에만 학습값 사용
                    if (sw.getSampleCount() >= 20) {
                        return sw.getWeight();
                    }
                    return defaultValue;
                })
                .orElse(defaultValue);
    }
}
