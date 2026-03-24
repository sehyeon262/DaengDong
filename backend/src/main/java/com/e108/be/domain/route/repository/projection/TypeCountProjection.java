package com.e108.be.domain.route.repository.projection;

import com.e108.be.domain.route.dto.response.RouteType;

/**
 * 경로 유형별 선택 횟수 Projection
 */
public interface TypeCountProjection {
    RouteType getSelectedType();
    Long getCount();
}
