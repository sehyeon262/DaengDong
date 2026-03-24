package com.e108.be.domain.walk.repository.projection;

import com.e108.be.domain.route.dto.response.RouteType;

/**
 * 경로 유형별 산책 통계 Projection
 *
 * 성능 평가 지표:
 * - 선택률: 각 유형의 산책 수 / 전체 산책 수
 * - 완주율: COMPLETED / (COMPLETED + CANCELED)
 * - 이탈률: CANCELED / (COMPLETED + CANCELED)
 */
public interface RouteTypeStatsProjection {

    /** 경로 유형 (null이면 자유 산책) */
    String getRouteType();

    /** 총 산책 수 */
    Long getTotalCount();

    /** 완료된 산책 수 (COMPLETED) */
    Long getCompletedCount();

    /** 취소된 산책 수 (CANCELED) */
    Long getCanceledCount();

    /** 평균 산책 거리 (미터) */
    Double getAvgDistanceM();

    /** 평균 산책 시간 (초) */
    Double getAvgDurationSec();

    /** 평균 경로 이탈률 (%) - 추천 경로 산책만 해당 */
    Double getAvgDeviationRate();
}
