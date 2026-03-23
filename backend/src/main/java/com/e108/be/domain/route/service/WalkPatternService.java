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
import java.util.EnumMap;
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
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkPatternService {

    private static final int ANALYSIS_DAYS = 30; // 최근 30일 분석

    private final WalkRecordRepository walkRecordRepository;
    private final RouteSelectionLogRepository selectionLogRepository;

    /**
     * 사용자의 산책 패턴 분석
     *
     * @param dogId 반려견 ID
     * @param memberId 회원 ID
     * @return 산책 패턴 데이터
     */
    public WalkPattern analyze(Long dogId, Long memberId) {
        LocalDateTime since = LocalDateTime.now().minusDays(ANALYSIS_DAYS);

        // 1. 완료된 산책 기록 조회
        List<WalkRecord> recentWalks = walkRecordRepository
                .findByDogIdAndWalkStatusAndStartTimeBetween(
                        dogId, WalkStatus.COMPLETED, since, LocalDateTime.now());

        // 2. 평균 산책 거리 (미터)
        double avgDistanceM = recentWalks.stream()
                .map(WalkRecord::getTotalDistance)
                .filter(d -> d != null && d.compareTo(BigDecimal.ZERO) > 0)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        // 3. 선호 시간대 (가장 많이 산책한 시)
        int preferredHour = recentWalks.stream()
                .filter(w -> w.getStartTime() != null)
                .map(w -> w.getStartTime().getHour())
                .reduce((a, b) -> a) // 첫 번째 값 기본
                .orElse(18); // 기본값: 저녁 6시

        if (!recentWalks.isEmpty()) {
            // 시간대별 빈도 계산
            Map<Integer, Long> hourCounts = new java.util.HashMap<>();
            recentWalks.stream()
                    .filter(w -> w.getStartTime() != null)
                    .forEach(w -> hourCounts.merge(w.getStartTime().getHour(), 1L, Long::sum));
            preferredHour = hourCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(18);
        }

        // 4. 산책 빈도
        int walkCount = recentWalks.size();

        // 5. 경로 선택 로그 기반 선호 유형
        RouteType preferredType = analyzePreferredType(memberId);

        // 6. 선택 로그 기반 평균 선호 거리
        Double selectionAvgDistance = selectionLogRepository.findAvgDistanceByMemberId(memberId);
        if (selectionAvgDistance != null && selectionAvgDistance > 0) {
            // 실제 산책 거리와 선택 거리의 가중 평균
            avgDistanceM = avgDistanceM > 0
                    ? (avgDistanceM * 0.6 + selectionAvgDistance * 0.4)
                    : selectionAvgDistance;
        }

        return new WalkPattern(avgDistanceM, preferredHour, preferredType, walkCount);
    }

    /**
     * 선택 로그에서 가장 많이 선택한 경로 유형 분석
     */
    private RouteType analyzePreferredType(Long memberId) {
        List<Object[]> typeCounts = selectionLogRepository.countByMemberIdGroupByType(memberId);

        if (typeCounts.isEmpty()) {
            return RouteType.RECOMMENDED; // 기본값
        }

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
     * 사용자 산책 패턴 데이터
     *
     * @param avgDistanceM  평균 산책 거리 (미터), 0이면 데이터 부족
     * @param preferredHour 선호 시간대 (0~23)
     * @param preferredType 선호 경로 유형
     * @param walkCount     최근 30일 산책 횟수
     */
    public record WalkPattern(
            double avgDistanceM,
            int preferredHour,
            RouteType preferredType,
            int walkCount
    ) {
        /**
         * 데이터가 충분한지 여부 (최소 3회 이상 산책)
         */
        public boolean hasEnoughData() {
            return walkCount >= 3;
        }
    }
}
