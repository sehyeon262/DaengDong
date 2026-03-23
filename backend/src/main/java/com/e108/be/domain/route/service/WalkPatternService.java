package com.e108.be.domain.route.service;

import com.e108.be.domain.route.dto.response.RouteType;
import com.e108.be.domain.route.repository.RouteSelectionLogRepository;
import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final WalkRecordRepository walkRecordRepository;
    private final RouteSelectionLogRepository selectionLogRepository;

    /**
     * 사용자의 산책 패턴 분석
     *
     * @param dogId    반려견 ID
     * @param memberId 회원 ID
     * @return 산책 패턴 데이터
     */
    public WalkPattern analyze(Long dogId, Long memberId) {
        LocalDateTime since = LocalDateTime.now().minusDays(ANALYSIS_DAYS);

        List<WalkRecord> recentWalks = walkRecordRepository
                .findByDogIdAndWalkStatusAndStartTimeBetween(
                        dogId, WalkStatus.COMPLETED, since, LocalDateTime.now());

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
                    ? (avgDistanceM * 0.6 + selectionAvgDistance * 0.4)
                    : selectionAvgDistance;
        }

        // 날씨별 선호 유형
        Map<String, RouteType> weatherPreferences = analyzeWeatherPreferences(memberId);

        return new WalkPattern(avgDistanceM, preferredHour, preferredType,
                walkCount, weatherPreferences);
    }

    private int analyzePreferredHour(List<WalkRecord> walks) {
        if (walks.isEmpty()) return 18;

        Map<Integer, Long> hourCounts = new HashMap<>();
        walks.stream()
                .filter(w -> w.getStartTime() != null)
                .forEach(w -> hourCounts.merge(w.getStartTime().getHour(), 1L, Long::sum));

        return hourCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(18);
    }

    private RouteType analyzePreferredType(Long memberId) {
        List<Object[]> typeCounts = selectionLogRepository.countByMemberIdGroupByType(memberId);
        if (typeCounts.isEmpty()) return RouteType.RECOMMENDED;

        RouteType maxType = RouteType.RECOMMENDED;
        long maxCount = 0;
        for (Object[] row : typeCounts) {
            RouteType type = (RouteType) row[0];
            long count = (long) row[1];
            if (count > maxCount) {
                maxCount = count;
                maxType = type;
            }
        }
        return maxType;
    }

    /**
     * 날씨별 선호 경로 유형 분석
     * 예: CLEAR → EXPLORE, RAIN → SHORT
     */
    private Map<String, RouteType> analyzeWeatherPreferences(Long memberId) {
        List<Object[]> rows = selectionLogRepository
                .countByMemberIdGroupByWeatherAndType(memberId);

        // weather → (type → count) 집계
        Map<String, Map<RouteType, Long>> weatherTypeCounts = new HashMap<>();
        for (Object[] row : rows) {
            String weather = (String) row[0];
            RouteType type = (RouteType) row[1];
            long count = (long) row[2];
            weatherTypeCounts
                    .computeIfAbsent(weather, k -> new HashMap<>())
                    .put(type, count);
        }

        // 각 날씨별 최다 선택 유형 추출
        Map<String, RouteType> result = new HashMap<>();
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
            Map<String, RouteType> weatherPreferences
    ) {
        public boolean hasEnoughData() {
            return walkCount >= 3;
        }
    }
}
