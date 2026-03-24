package com.e108.be.domain.route.repository.projection;

/**
 * 카테고리별 선택 횟수 Projection
 */
public interface CategoryCountProjection {
    Integer getCategoryId();
    String getCategoryName();
    Long getCount();
}
