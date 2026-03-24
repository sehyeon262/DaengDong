package com.e108.be.domain.route.repository;

import com.e108.be.domain.route.entity.RouteSelectionPlace;
import com.e108.be.domain.route.repository.projection.CategoryCountProjection;
import com.e108.be.domain.route.repository.projection.PlaceCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteSelectionPlaceRepository extends JpaRepository<RouteSelectionPlace, Long> {

    /**
     * 사용자가 선택한 경로에 포함된 장소의 카테고리별 선택 횟수
     * -> UserCategoryPreference 집계에 활용
     */
    @Query("""
        SELECT p.category.id AS categoryId, p.category.name AS categoryName, COUNT(sp) AS count
        FROM RouteSelectionPlace sp
        JOIN sp.place p
        JOIN sp.selectionLog sl
        WHERE sl.memberId = :memberId
        GROUP BY p.category.id, p.category.name
        ORDER BY COUNT(sp) DESC
        """)
    List<CategoryCountProjection> countByMemberIdGroupByCategory(@Param("memberId") Long memberId);

    /**
     * 사용자가 특정 장소를 선택한 횟수 (재방문 패턴 분석)
     */
    @Query("""
        SELECT sp.place.id AS placeId, COUNT(sp) AS count
        FROM RouteSelectionPlace sp
        JOIN sp.selectionLog sl
        WHERE sl.memberId = :memberId
        GROUP BY sp.place.id
        ORDER BY COUNT(sp) DESC
        """)
    List<PlaceCountProjection> countByMemberIdGroupByPlace(@Param("memberId") Long memberId);
}
