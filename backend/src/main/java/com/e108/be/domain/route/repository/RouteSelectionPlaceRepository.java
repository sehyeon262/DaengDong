package com.e108.be.domain.route.repository;

import com.e108.be.domain.route.entity.RouteSelectionPlace;
import com.e108.be.domain.route.repository.projection.CategoryCountProjection;
import com.e108.be.domain.route.repository.projection.PlaceCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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

    /**
     * 최근 N일 내 사용자가 선택한 경로에 포함된 장소 ID 목록
     * 피로도(Decay) 계산에 활용: 최근 방문 장소는 스코어 감점
     */
    @Query("""
        SELECT DISTINCT sp.place.id
        FROM RouteSelectionPlace sp
        JOIN sp.selectionLog sl
        WHERE sl.memberId = :memberId AND sl.createdAt >= :since
        """)
    Set<Long> findRecentPlaceIds(@Param("memberId") Long memberId, @Param("since") LocalDateTime since);
}
