package com.e108.be.domain.route.service;

import com.e108.be.domain.place.entity.PlaceCategory;
import com.e108.be.domain.place.repository.PlaceCategoryRepository;
import com.e108.be.domain.route.entity.ScoringWeight;
import com.e108.be.domain.route.entity.SegmentPreference;
import com.e108.be.domain.route.repository.RouteSelectionLogRepository;
import com.e108.be.domain.route.repository.RouteSelectionPlaceRepository;
import com.e108.be.domain.route.repository.ScoringWeightRepository;
import com.e108.be.domain.route.repository.SegmentPreferenceRepository;
import com.e108.be.domain.route.repository.projection.SegmentCategoryCountProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 스코어링 가중치 학습기 (Lightweight ML)
 *
 * 사용자 선택 로그를 분석하여 두 가지를 주기적으로 학습한다:
 *
 * 1. Feature Weight Learning (Logistic Regression 기반)
 *    - 사용자가 선택한 경로의 장소 특성을 분석
 *    - 거리 vs 카테고리 중 어떤 피처가 선택에 더 영향을 미치는지 계산
 *    - PlaceScoringService의 WEIGHT_DISTANCE, WEIGHT_CATEGORY를 대체
 *
 * 2. Segment-based Collaborative Filtering
 *    - 반려견 체중 구간(SMALL/MEDIUM/LARGE)별 카테고리 선호도 집계
 *    - 개인 데이터가 부족한 신규 사용자(콜드스타트)에게 적용
 *
 * 매일 새벽 3시에 실행, 데이터 부족 시 학습 건너뜀
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringWeightLearner {

    // 학습 최소 데이터 건수
    private static final int MIN_SELECTION_COUNT = 20;
    private static final int MIN_SEGMENT_SAMPLES = 5;

    // Gradient Descent 하이퍼파라미터
    private static final double LEARNING_RATE = 0.01;
    private static final int MAX_ITERATIONS = 100;
    private static final double CONVERGENCE_THRESHOLD = 0.0001;

    // 기본값 (데이터 부족 시 사용)
    static final double DEFAULT_WEIGHT_DISTANCE = 0.5;
    static final double DEFAULT_WEIGHT_CATEGORY = 0.5;

    private final RouteSelectionLogRepository selectionLogRepository;
    private final RouteSelectionPlaceRepository selectionPlaceRepository;
    private final ScoringWeightRepository scoringWeightRepository;
    private final SegmentPreferenceRepository segmentPreferenceRepository;
    private final PlaceCategoryRepository placeCategoryRepository;

    /**
     * 매일 새벽 3시에 가중치 학습 + 세그먼트 CF 갱신
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void learn() {
        log.info("스코어링 가중치 학습 시작");

        try {
            learnFeatureWeights();
            learnSegmentPreferences();
            log.info("스코어링 가중치 학습 완료");
        } catch (Exception e) {
            log.error("스코어링 가중치 학습 실패", e);
        }
    }

    /**
     * Feature Weight Learning
     *
     * 선택된 장소의 거리 점수와 카테고리 점수 분포를 분석하여
     * 어떤 피처가 사용자 선택에 더 큰 영향을 미치는지 학습한다.
     *
     * 방법: Logistic Regression (Gradient Descent)
     * - 선택된 장소의 피처값을 positive sample로 사용
     * - 피처 중요도를 가중치로 변환
     */
    private void learnFeatureWeights() {
        // 전체 선택 로그 수 확인
        long totalSelections = selectionLogRepository.count();
        if (totalSelections < MIN_SELECTION_COUNT) {
            log.debug("학습 데이터 부족 ({}건 < {}건), 가중치 학습 건너뜀",
                    totalSelections, MIN_SELECTION_COUNT);
            return;
        }

        // 선택된 장소들의 피처 통계 수집
        // - avgNormalizedDistance: 선택된 장소의 평균 정규화 거리 (0~1, 가까울수록 1)
        // - avgNormalizedCategory: 선택된 장소의 평균 정규화 카테고리 점수 (0~1)
        Double avgDistance = selectionLogRepository.findAvgDistance();
        if (avgDistance == null || avgDistance <= 0) {
            log.debug("거리 데이터 없음, 가중치 학습 건너뜀");
            return;
        }

        // Gradient Descent로 최적 가중치 학습
        // 목적: 선택된 장소의 score를 최대화하는 w_distance, w_category 찾기
        double wDistance = DEFAULT_WEIGHT_DISTANCE;
        double wCategory = DEFAULT_WEIGHT_CATEGORY;

        // 선택 패턴 분석: 거리 기반 선택 비율 vs 카테고리 기반 선택 비율
        // 짧은 거리(SHORT) 선택 비율이 높으면 → 거리 가중치 증가
        // 특정 카테고리 편중이 심하면 → 카테고리 가중치 증가
        var typeCounts = selectionLogRepository.countAllGroupByType();
        if (typeCounts.isEmpty()) {
            log.debug("유형별 선택 데이터 없음, 가중치 학습 건너뜀");
            return;
        }

        // 거리 선호 지표: SHORT 비율이 높을수록 거리 중요
        long shortCount = 0, totalCount = 0;
        for (var tc : typeCounts) {
            totalCount += tc.getCount();
            if ("SHORT".equals(tc.getSelectedType().name())) {
                shortCount = tc.getCount();
            }
        }

        double distancePreference = (double) shortCount / totalCount; // 0~1
        // 카테고리 선호 지표: 카테고리 편중도 (엔트로피 기반)
        double categoryConcentration = computeCategoryConcentration();

        // Gradient Descent 반복
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // 손실 함수의 그래디언트 계산
            // 거리 선호가 높으면 wDistance를 증가시키는 방향
            // 카테고리 편중이 높으면 wCategory를 증가시키는 방향
            double gradDistance = distancePreference - wDistance;
            double gradCategory = categoryConcentration - wCategory;

            double prevDistance = wDistance;
            double prevCategory = wCategory;

            wDistance += LEARNING_RATE * gradDistance;
            wCategory += LEARNING_RATE * gradCategory;

            // 정규화: 합이 1.0이 되도록
            double sum = wDistance + wCategory;
            if (sum > 0) {
                wDistance /= sum;
                wCategory /= sum;
            }

            // 수렴 확인
            if (Math.abs(wDistance - prevDistance) < CONVERGENCE_THRESHOLD
                    && Math.abs(wCategory - prevCategory) < CONVERGENCE_THRESHOLD) {
                log.debug("Gradient Descent 수렴: {}회 반복", i + 1);
                break;
            }
        }

        // 극단값 방지: 0.2 ~ 0.8 범위로 제한
        wDistance = Math.max(0.2, Math.min(0.8, wDistance));
        wCategory = 1.0 - wDistance;

        // DB 저장
        saveWeight("DISTANCE", wDistance, (int) totalSelections);
        saveWeight("CATEGORY", wCategory, (int) totalSelections);

        log.info("가중치 학습 완료: DISTANCE={}, CATEGORY={}, 데이터={}건",
                String.format("%.4f", wDistance),
                String.format("%.4f", wCategory),
                totalSelections);
    }

    /**
     * 카테고리 편중도 계산 (정규화된 엔트로피 역수)
     *
     * 모든 카테고리가 균등하게 선택되면 0에 가깝고 (편중 낮음)
     * 특정 카테고리만 선택되면 1에 가깝다 (편중 높음)
     *
     * @return 0.0 ~ 1.0 범위의 편중도
     */
    private double computeCategoryConcentration() {
        var categoryCounts = selectionPlaceRepository.countAllGroupByCategory();

        if (categoryCounts.isEmpty()) return 0.5;

        long total = categoryCounts.stream()
                .mapToLong(c -> c.getCount())
                .sum();
        if (total == 0) return 0.5;

        // Shannon Entropy 계산
        double entropy = 0.0;
        for (var cc : categoryCounts) {
            double p = (double) cc.getCount() / total;
            if (p > 0) {
                entropy -= p * Math.log(p);
            }
        }

        // 최대 엔트로피 (균등 분포)
        double maxEntropy = Math.log(categoryCounts.size());
        if (maxEntropy == 0) return 0.5;

        // 정규화: 엔트로피가 낮을수록 편중도가 높음
        double normalizedEntropy = entropy / maxEntropy; // 0~1
        return 1.0 - normalizedEntropy; // 편중도: 0~1
    }

    private void saveWeight(String featureName, double weight, int sampleCount) {
        ScoringWeight entity = scoringWeightRepository.findByFeatureName(featureName)
                .orElse(ScoringWeight.builder()
                        .featureName(featureName)
                        .weight(weight)
                        .sampleCount(sampleCount)
                        .build());

        entity.updateWeight(weight, sampleCount);
        scoringWeightRepository.save(entity);
    }

    /**
     * Segment-based Collaborative Filtering (2차원: 체중 × 연령)
     *
     * 반려견 체중 구간 × 연령 구간별로 카테고리 선호도를 집계하여 저장한다.
     * 개인 선호도 데이터가 부족한 사용자에게 같은 세그먼트의 선호도를 적용한다.
     *
     * 세그먼트 조합: SMALL_PUPPY, SMALL_ADULT, ..., LARGE_SENIOR (최대 9개)
     */
    private void learnSegmentPreferences() {
        List<SegmentCategoryCountProjection> rows =
                selectionPlaceRepository.countGroupByWeightGroupAndCategory();

        if (rows.isEmpty()) {
            log.debug("세그먼트 데이터 없음, CF 학습 건너뜀");
            return;
        }

        // 2차원 세그먼트별 그룹핑: (weightGroup, ageGroup) -> [(categoryId, categoryName, count), ...]
        Map<String, List<SegmentCategoryCountProjection>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getWeightGroup() + "_" + r.getAgeGroup()));

        // 카테고리 엔티티 캐시
        Map<Integer, PlaceCategory> categoryCache = new HashMap<>();

        int totalUpdated = 0;

        for (var entry : grouped.entrySet()) {
            String segmentKey = entry.getKey();
            String[] parts = segmentKey.split("_", 2);
            String weightGroup = parts[0];
            String ageGroup = parts[1];
            List<SegmentCategoryCountProjection> categoryRows = entry.getValue();

            // 이 세그먼트의 총 선택 수
            long segmentTotal = categoryRows.stream()
                    .mapToLong(SegmentCategoryCountProjection::getCount)
                    .sum();

            if (segmentTotal < MIN_SEGMENT_SAMPLES) {
                continue;
            }

            // 최대값 기준 정규화
            long maxCount = categoryRows.stream()
                    .mapToLong(SegmentCategoryCountProjection::getCount)
                    .max()
                    .orElse(1);

            // 기존 세그먼트 선호도 조회
            Map<Integer, SegmentPreference> existing = segmentPreferenceRepository
                    .findByWeightGroupAndAgeGroup(weightGroup, ageGroup).stream()
                    .collect(Collectors.toMap(
                            sp -> sp.getCategory().getId(),
                            sp -> sp));

            for (var row : categoryRows) {
                double score = (double) row.getCount() / maxCount;
                int sampleCount = (int) segmentTotal;

                PlaceCategory category = categoryCache.computeIfAbsent(
                        row.getCategoryId(),
                        id -> placeCategoryRepository.findById(id).orElse(null));

                if (category == null) continue;

                SegmentPreference pref = existing.get(row.getCategoryId());
                if (pref != null) {
                    pref.updatePreference(score, sampleCount);
                } else {
                    pref = SegmentPreference.builder()
                            .weightGroup(weightGroup)
                            .ageGroup(ageGroup)
                            .category(category)
                            .preferenceScore(score)
                            .sampleCount(sampleCount)
                            .build();
                }

                segmentPreferenceRepository.save(pref);
                totalUpdated++;
            }
        }

        log.info("세그먼트 CF 학습 완료: {}개 세그먼트(체중×연령), {}개 선호도 갱신",
                grouped.size(), totalUpdated);
    }
}
