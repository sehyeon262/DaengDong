package com.e108.be.domain.route.repository.projection;

import com.e108.be.domain.route.dto.response.RouteType;
import com.e108.be.domain.route.entity.WeatherCondition;

/**
 * 날씨별 경로 유형 선택 횟수 Projection
 */
public interface WeatherTypeCountProjection {
    WeatherCondition getWeatherCondition();
    RouteType getSelectedType();
    Long getCount();
}
