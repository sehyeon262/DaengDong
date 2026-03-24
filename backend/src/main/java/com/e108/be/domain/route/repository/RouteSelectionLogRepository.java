package com.e108.be.domain.route.repository;

import com.e108.be.domain.route.entity.RouteSelectionLog;
import com.e108.be.domain.route.repository.projection.HourCountProjection;
import com.e108.be.domain.route.repository.projection.TypeCountProjection;
import com.e108.be.domain.route.repository.projection.WeatherTypeCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteSelectionLogRepository extends JpaRepository<RouteSelectionLog, Long> {

    /**
     * 사용자의 경로 유형별 선택 횟수
     */
    @Query("""
        SELECT r.selectedType AS selectedType, COUNT(r) AS count
        FROM RouteSelectionLog r
        WHERE r.memberId = :memberId
        GROUP BY r.selectedType
        """)
    List<TypeCountProjection> countByMemberIdGroupByType(@Param("memberId") Long memberId);

    /**
     * 사용자의 시간대별 선택 횟수
     */
    @Query("""
        SELECT r.hourOfDay AS hourOfDay, COUNT(r) AS count
        FROM RouteSelectionLog r
        WHERE r.memberId = :memberId
        GROUP BY r.hourOfDay
        ORDER BY COUNT(r) DESC
        """)
    List<HourCountProjection> countByMemberIdGroupByHour(@Param("memberId") Long memberId);

    /**
     * 사용자의 평균 선택 거리 (미터)
     */
    @Query("""
        SELECT AVG(r.selectedDistanceM)
        FROM RouteSelectionLog r
        WHERE r.memberId = :memberId
        """)
    Double findAvgDistanceByMemberId(@Param("memberId") Long memberId);

    /**
     * 사용자의 날씨별 경로 유형 선택 횟수
     */
    @Query("""
        SELECT r.weatherCondition AS weatherCondition, r.selectedType AS selectedType, COUNT(r) AS count
        FROM RouteSelectionLog r
        WHERE r.memberId = :memberId AND r.weatherCondition IS NOT NULL
        GROUP BY r.weatherCondition, r.selectedType
        """)
    List<WeatherTypeCountProjection> countByMemberIdGroupByWeatherAndType(@Param("memberId") Long memberId);

    long countByMemberId(Long memberId);

    /**
     * 전체 사용자의 경로 유형별 선택 횟수 (ML 학습용)
     */
    @Query("""
        SELECT r.selectedType AS selectedType, COUNT(r) AS count
        FROM RouteSelectionLog r
        GROUP BY r.selectedType
        """)
    List<TypeCountProjection> countAllGroupByType();

    /**
     * 전체 사용자의 평균 선택 거리 (ML 학습용)
     */
    @Query("SELECT AVG(r.selectedDistanceM) FROM RouteSelectionLog r")
    Double findAvgDistance();
}
