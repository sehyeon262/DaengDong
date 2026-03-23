package com.e108.be.domain.route.repository;

import com.e108.be.domain.route.dto.response.RouteType;
import com.e108.be.domain.route.entity.RouteSelectionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteSelectionLogRepository extends JpaRepository<RouteSelectionLog, Long> {

    /**
     * 사용자의 경로 유형별 선택 횟수
     * 예: SHORT=15, RECOMMENDED=30, EXPLORE=5
     */
    @Query("""
        SELECT r.selectedType, COUNT(r)
        FROM RouteSelectionLog r
        WHERE r.memberId = :memberId
        GROUP BY r.selectedType
        """)
    List<Object[]> countByMemberIdGroupByType(@Param("memberId") Long memberId);

    /**
     * 사용자의 시간대별 선택 횟수
     * 예: 7시=10, 8시=15, 19시=20
     */
    @Query("""
        SELECT r.hourOfDay, COUNT(r)
        FROM RouteSelectionLog r
        WHERE r.memberId = :memberId
        GROUP BY r.hourOfDay
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> countByMemberIdGroupByHour(@Param("memberId") Long memberId);

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
     * 사용자의 총 선택 로그 수
     */
    long countByMemberId(Long memberId);
}
