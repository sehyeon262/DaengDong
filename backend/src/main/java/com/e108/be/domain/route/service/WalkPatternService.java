package com.e108.be.domain.route.service;

import com.e108.be.domain.route.dto.response.RouteType;
import com.e108.be.domain.route.entity.WeatherCondition;
import com.e108.be.domain.route.repository.RouteSelectionLogRepository;
import com.e108.be.domain.route.repository.projection.TypeCountProjection;
import com.e108.be.domain.route.repository.projection.WeatherTypeCountProjection;
import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사용자 산책 패턴 분석 서비스
 *
 * WalkRecord와 RouteSelectionLog를 분석하여
 * 사용자의 산책 선호 패턴을 추출한다.
 *
 * 분석 항목:
 * - 평균 산책 거리 (미터)
 * - 선호 시간대 (시)
 * - 선호 경로 유형 (SHORT / RECOMMENDED / EXPLORE)
 * - 산책 빈도 (최근 30일 기준)
 * - 날씨별 선호 패턴
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkPatternService {

    private static final int ANALYSIS_DAYS = 30;
    private static final int DEFAULT_PREFERRED_HOUR = 18;
    private static final int MIN_WALKS_FOR_ANALYSIS = 3;
    private static final double ACTUAL_WALK_WEIGHT = 0.6;
    private static final double SELECTION_LOG_WEIGHT = 0.4;

    private final WalkRecordRepository walkRecordRepository;
    private final RouteSelectionLogRepository selectionLogRepository;

    /**
     * 사용자의 산책 패턴 분석
     *
     * 결과는 Redis에 캐싱되며, 경로 선택(logSelection) 시 자동 무효화된다.
     *
     * @param dogId    반려견 ID
     * @param memberId 회원 ID
     * @return 산책 패턴 데이터
     */
    @Cacheable(value = "walkPattern", key = "#memberId")
    public WalkPattern analyze(Long dogId, Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(ANALYSIS_DAYS);

        List<WalkRecord> recentWalks = walkRecordRepository
                .findByDogIdAndWalkStatusAndStartTimeBetween(
                        dogId, WalkStatus.COMPLETED, since, now);

        // 평균 산책 거리 (미터)
        double avgDistanceM = recentWalks.stream()
                .map(WalkRecord::getTotalDistance)
                .filter(d -> d != null && d.compareTo(BigDecimal.ZERO) > 0)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        // 선호 시간대
        int preferredHour = analyzePreferredHour(recentWalks);

        // 산책 빈도
        int walkCount = recentWalks.size();

        // 선호 경로 유형
        RouteType preferredType = analyzePreferredType(memberId);

        // 선택 로그 기반 평균 거리와 가중 결합
        Double selectionAvgDistance = selectionLogRepository.findAvgDistanceByMemberId(memberId);
        if (selectionAvgDistance != null && selectionAvgDistance > 0) {
            avgDistanceM = avgDistanceM > 0
                    ? (avgDistanceM * ACTUAL_WALK_WEIGHT + selectionAvgDistance * SELECTION_LOG_WEIGHT)
                    : selectionAvgDistance;
        }

        // 날씨별 선호 유형
        Map<WeatherCondition, RouteType> weatherPreferences = analyzeWeatherPreferences(memberId);

        return new WalkPattern(avgDistanceM, preferredHour, preferredType,
                walkCount, weatherPreferences);
    }

    private int analyzePreferredHour(List<WalkRecord> walks) {
        if (walks.isEmpty()) return DEFAULT_PREFERRED_HOUR;

        Map<Integer, Long> hourCounts = new HashMap<>();
        walks.stream()
                .filter(w -> w.getStartTime() != null)
                .forEach(w -> hourCounts.merge(w.getStartTime().getHour(), 1L, Long::sum));

        return hourCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DEFAULT_PREFERRED_HOUR);
    }

    private RouteType analyzePreferredType(Long memberId) {
        List<TypeCountProjection> typeCounts = selectionLogRepository.countByMemberIdGroupByType(memberId);

        return typeCounts.stream()
                .max(Comparator.comparing(TypeCountProjection::getCount))
                .map(TypeCountProjection::getSelectedType)
                .orElse(RouteType.RECOMMENDED);
    }

    /**
     * 날씨별 선호 경로 유형 분석
     * 예: CLEAR -> EXPLORE, RAIN -> SHORT
     */
    private Map<WeatherCondition, RouteType> analyzeWeatherPreferences(Long memberId) {
        List<WeatherTypeCountProjection> rows = selectionLogRepository
                .countByMemberIdGroupByWeatherAndType(memberId);

        // weather -> (type -> count) 집계
        Map<WeatherCondition, Map<RouteType, Long>> weatherTypeCounts = new HashMap<>();
        for (WeatherTypeCountProjection row : rows) {
            weatherTypeCounts
                    .computeIfAbsent(row.getWeatherCondition(), k -> new HashMap<>())
                    .put(row.getSelectedType(), row.getCount());
        }

        // 각 날씨별 최다 선택 유형 추출
        Map<WeatherCondition, RouteType> result = new HashMap<>();
        for (var entry : weatherTypeCounts.entrySet()) {
            entry.getValue().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresent(e -> result.put(entry.getKey(), e.getKey()));
        }
        return result;
    }

    /**
     * @param avgDistanceM       평균 산책 거리 (미터)
     * @param preferredHour      선호 시간대 (0~23)
     * @param preferredType      선호 경로 유형
     * @param walkCount          최근 30일 산책 횟수
     * @param weatherPreferences 날씨별 선호 경로 유형
     */
    public record WalkPattern(
            double avgDistanceM,
            int preferredHour,
            RouteType preferredType,
            int walkCount,
            Map<WeatherCondition, RouteType> weatherPreferences
    ) {
        /**
         * 데이터가 충분한지 여부
         */
        public boolean hasEnoughData() {
            return walkCount >= MIN_WALKS_FOR_ANALYSIS;
        }
    }
}
