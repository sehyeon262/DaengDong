package com.e108.be.domain.route.repository.projection;

/**
 * 시간대별 선택 횟수 Projection
 */
public interface HourCountProjection {
    Integer getHourOfDay();
    Long getCount();
}
