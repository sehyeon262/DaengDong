package com.e108.be.domain.route.repository.projection;

/**
 * 장소별 선택 횟수 Projection (재방문 패턴 분석)
 */
public interface PlaceCountProjection {
    Long getPlaceId();
    Long getCount();
}
