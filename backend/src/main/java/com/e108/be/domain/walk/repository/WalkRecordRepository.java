package com.e108.be.domain.walk.repository;

import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import com.e108.be.domain.walk.repository.projection.RouteTypeStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WalkRecordRepository extends JpaRepository<WalkRecord, Long> {
    Optional<WalkRecord> findByDogIdAndWalkStatus(Long dogId, WalkStatus walkStatus);

    List<WalkRecord> findByDogIdAndWalkStatusAndStartTimeBetween(
            Long dogId, WalkStatus walkStatus,
            LocalDateTime start, LocalDateTime end
    );

    List<WalkRecord> findAllByWalkStatus(WalkStatus walkStatus);

    // 특정 강아지의 완료된 산책 목록 (최신순) - 히트맵/기록 조회용
    List<WalkRecord> findAllByDogIdAndWalkStatusOrderByStartTimeDesc(Long dogId, WalkStatus walkStatus);

    /**
     * GPS 배치(WKT)를 route_line에 이어붙임
     * - route_line이 NULL이면 새 LINESTRING 생성
     * - 이미 있으면 기존 LINESTRING 끝에 새 좌표 추가
     * WKT 예시: "LINESTRING(126.97 37.56, 126.98 37.57)"
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE walk_records SET route_line =
            CASE WHEN route_line IS NULL
                THEN ST_GeomFromText(:wkt, 4326)::geography
                ELSE ST_MakeLine(route_line::geometry, ST_GeomFromText(:wkt, 4326))::geography
            END
            WHERE id = :walkId
            """, nativeQuery = true)
    void appendToRouteLine(@Param("walkId") Long walkId, @Param("wkt") String wkt);

    /**
     * route_line 전체 길이 조회 (단위: 미터)
     * GEOGRAPHY 타입에서 ST_Length는 미터 반환
     * route_line이 NULL이면 null 반환
     */
    @Query(value = "SELECT ST_Length(route_line) FROM walk_records WHERE id = :walkId", nativeQuery = true)
    Double getRouteLength(@Param("walkId") Long walkId);

    /**
     * route_line의 마지막 좌표(현재 위치) 반환
     * 반환: List<Object[]> — 각 행이 [위도(lat), 경도(lon)]
     * route_line이 NULL이거나 walk가 없으면 빈 리스트 반환
     *
     * ※ Hibernate 6(Spring Boot 3.x)에서 Object[] 단일 반환 시
     *    Object[]{Object[]{lat,lon}} 으로 래핑되는 이슈 → List<Object[]> 로 변경
     */
    @Query(value = """
            SELECT ST_Y(ST_EndPoint(route_line::geometry)) AS lat,
                   ST_X(ST_EndPoint(route_line::geometry)) AS lon
            FROM walk_records
            WHERE id = :walkId AND route_line IS NOT NULL
            """, nativeQuery = true)
    List<Object[]> getLastPoint(@Param("walkId") Long walkId);

    /**
     * 추천 경로(recommended_route) 저장
     * 산책 시작 시 TMap 경로 좌표를 LINESTRING으로 저장
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE walk_records
            SET recommended_route = ST_GeomFromText(:wkt, 4326)::geography
            WHERE id = :walkId
            """, nativeQuery = true)
    void saveRecommendedRoute(@Param("walkId") Long walkId, @Param("wkt") String wkt);

    /**
     * 경로 이탈률 계산 (단위: %)
     *
     * 실제 경로(route_line)의 각 포인트 중 추천 경로(recommended_route)에서
     * 50m 이상 벗어난 포인트의 비율을 계산
     *
     * 0% = 완벽하게 추천 경로를 따름
     * 100% = 전혀 따르지 않음
     */
    @Query(value = """
            WITH points AS (
                SELECT (ST_DumpPoints(route_line::geometry)).geom AS pt
                FROM walk_records
                WHERE id = :walkId AND route_line IS NOT NULL
            ),
            total AS (
                SELECT COUNT(*) AS cnt FROM points
            ),
            deviated AS (
                SELECT COUNT(*) AS cnt FROM points p, walk_records w
                WHERE w.id = :walkId
                  AND NOT ST_DWithin(
                      p.pt::geography,
                      w.recommended_route,
                      :bufferM
                  )
            )
            SELECT CASE WHEN t.cnt = 0 THEN NULL
                        ELSE ROUND((d.cnt * 100.0 / t.cnt)::numeric, 2)
                   END
            FROM total t, deviated d
            """, nativeQuery = true)
    Double calculateDeviationRate(@Param("walkId") Long walkId, @Param("bufferM") double bufferM);

    // ── 배지용 카운트 쿼리 ──

    @Query("SELECT COUNT(w) FROM WalkRecord w WHERE w.dogId IN :dogIds AND w.walkStatus = com.e108.be.domain.walk.entity.WalkStatus.COMPLETED")
    long countCompletedWalksByDogIds(@Param("dogIds") List<Long> dogIds);

    @Query(value = "SELECT COUNT(*) FROM walk_records WHERE dog_id IN (:dogIds) AND walk_status = 'COMPLETED' AND route_line IS NOT NULL", nativeQuery = true)
    long countWalksWithRoute(@Param("dogIds") List<Long> dogIds);

    @Query(value = """
            SELECT COUNT(*) FROM walk_records w
            JOIN diaries d ON d.walk_id = w.id
            WHERE w.dog_id IN (:dogIds)
              AND w.walk_status = 'COMPLETED'
              AND w.photo_urls IS NOT NULL
              AND w.photo_urls::text != '[]'
            """, nativeQuery = true)
    long countWalksWithPhotoAndDiary(@Param("dogIds") List<Long> dogIds);

    @Query(value = "SELECT COALESCE(SUM(json_array_length(photo_urls)), 0) FROM walk_records WHERE dog_id IN (:dogIds) AND photo_urls IS NOT NULL", nativeQuery = true)
    long countTotalPhotos(@Param("dogIds") List<Long> dogIds);

    // ── 성능 평가: 경로 유형별 통계 ──

    /**
     * 경로 유형별 산책 통계 (선택률, 완주율, 이탈률 산출용)
     *
     * route_type이 null인 행 = 자유 산책
     * route_type이 있는 행 = 경로 추천 기반 산책
     */
    @Query("""
        SELECT w.routeType AS routeType,
               COUNT(w) AS totalCount,
               SUM(CASE WHEN w.walkStatus = com.e108.be.domain.walk.entity.WalkStatus.COMPLETED THEN 1 ELSE 0 END) AS completedCount,
               SUM(CASE WHEN w.walkStatus = com.e108.be.domain.walk.entity.WalkStatus.CANCELED THEN 1 ELSE 0 END) AS canceledCount,
               AVG(w.totalDistance) AS avgDistanceM,
               AVG(w.totalDuration) AS avgDurationSec,
               AVG(w.deviationRate) AS avgDeviationRate
        FROM WalkRecord w
        GROUP BY w.routeType
        """)
    List<RouteTypeStatsProjection> getRouteTypeStats();

    /**
     * 현재 좌표(lat, lon) 기준 반경(radiusM) 내 진행 중인 산책 세션 조회.
     * - PostGIS ST_DWithin으로 반경 필터링
     * - ST_Distance로 정확한 거리 계산
     * - route_line의 마지막 좌표(현재 위치) 반환
     * - 자기 자신(myDogId)의 산책 세션 제외
     * - route_line이 없는 세션 제외
     *
     * @return List<Object[]> — 각 행이 [walkRecordId, dogId, lat, lon, distanceM]
     */
    @Query(value = """
            SELECT
                w.id AS walkRecordId,
                w.dog_id AS dogId,
                ST_Y(ST_EndPoint(w.route_line::geometry)) AS lat,
                ST_X(ST_EndPoint(w.route_line::geometry)) AS lon,
                ST_Distance(
                    w.route_line,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                ) AS distanceM
            FROM walk_records w
            WHERE w.walk_status = 'IN_PROGRESS'
              AND w.route_line IS NOT NULL
              AND w.dog_id <> :myDogId
              AND ST_DWithin(
                    w.route_line,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                    :radiusM
                  )
            ORDER BY distanceM ASC
            """, nativeQuery = true)
    List<Object[]> findNearbyInProgressWalks(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusM") double radiusM,
            @Param("myDogId") Long myDogId
    );
}
