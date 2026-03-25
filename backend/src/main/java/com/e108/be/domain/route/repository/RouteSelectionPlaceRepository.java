package com.e108.be.domain.route.repository;

import com.e108.be.domain.route.entity.RouteSelectionPlace;
import com.e108.be.domain.route.repository.projection.CategoryCountProjection;
import com.e108.be.domain.route.repository.projection.PlaceCountProjection;
import com.e108.be.domain.route.repository.projection.SegmentCategoryCountProjection;
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
        WHERE sl.userId = :userId
        GROUP BY p.category.id, p.category.name
        ORDER BY COUNT(sp) DESC
        """)
    List<CategoryCountProjection> countByUserIdGroupByCategory(@Param("userId") Long userId);

    /**
     * 사용자가 특정 장소를 선택한 횟수 (재방문 패턴 분석)
     */
    @Query("""
        SELECT sp.place.id AS placeId, COUNT(sp) AS count
        FROM RouteSelectionPlace sp
        JOIN sp.selectionLog sl
        WHERE sl.userId = :userId
        GROUP BY sp.place.id
        ORDER BY COUNT(sp) DESC
        """)
    List<PlaceCountProjection> countByUserIdGroupByPlace(@Param("userId") Long userId);

    /**
     * 최근 N일 내 사용자가 선택한 경로에 포함된 장소 ID 목록
     * 피로도(Decay) 계산에 활용: 최근 방문 장소는 스코어 감점
     */
    @Query("""
        SELECT DISTINCT sp.place.id
        FROM RouteSelectionPlace sp
        JOIN sp.selectionLog sl
        WHERE sl.userId = :userId AND sl.createdAt >= :since
        """)
    Set<Long> findRecentPlaceIds(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * 체중 구간별 카테고리 선택 횟수 (세그먼트 CF 학습용)
     *
     * Dog의 체중을 기준으로 SMALL/MEDIUM/LARGE 세그먼트를 분류하고,
     * 각 세그먼트가 선택한 장소의 카테고리별 횟수를 집계한다.
     */
    @Query("""
        SELECT CASE
                   WHEN d.weight < 10 THEN 'SMALL'
                   WHEN d.weight < 25 THEN 'MEDIUM'
                   ELSE 'LARGE'
               END AS weightGroup,
               p.category.id AS categoryId,
               p.category.name AS categoryName,
               COUNT(sp) AS count
        FROM RouteSelectionPlace sp
        JOIN sp.place p
        JOIN sp.selectionLog sl
        JOIN Dog d ON d.user.id = sl.userId
        WHERE p.category IS NOT NULL AND d.weight IS NOT NULL
        GROUP BY weightGroup, p.category.id, p.category.name
        ORDER BY weightGroup, COUNT(sp) DESC
        """)
    List<SegmentCategoryCountProjection> countGroupByWeightGroupAndCategory();

    /**
     * 전체 사용자의 카테고리별 선택 횟수 (ML 학습용)
     */
    @Query("""
        SELECT p.category.id AS categoryId, p.category.name AS categoryName, COUNT(sp) AS count
        FROM RouteSelectionPlace sp
        JOIN sp.place p
        WHERE p.category IS NOT NULL
        GROUP BY p.category.id, p.category.name
        ORDER BY COUNT(sp) DESC
        """)
    List<CategoryCountProjection> countAllGroupByCategory();
}
