package com.e108.be.domain.route.repository.projection;

/**
 * 세그먼트(체중 구간)별 카테고리 선택 횟수 Projection
 */
public interface SegmentCategoryCountProjection {

    String getWeightGroup();

    Integer getCategoryId();

    String getCategoryName();

    Long getCount();
}
