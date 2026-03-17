package com.e108.be.domain.place.repository;

/**
 * 주변 장소 네이티브 쿼리 결과 매핑용 Projection
 *
 * 컬럼 alias가 getter 이름(get 제외, 첫 글자 소문자)과 일치해야 매핑됨
 */
public interface NearbyPlaceProjection {
    Long getId();
    String getName();
    String getAddress();
    String getContact();
    String getImageUrl();       // SQL alias: imageUrl
    Double getLatitude();
    Double getLongitude();
    String getCategoryName();   // SQL alias: categoryName
    Integer getRouteWeight();   // SQL alias: routeWeight
    Double getDistanceMeters(); // SQL alias: distanceMeters
}
