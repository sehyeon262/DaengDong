package com.e108.be.domain.walk.repository;

import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
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
     * 반환: [위도(lat), 경도(lon)]
     * route_line이 NULL이면 null 반환
     */
    @Query(value = """
            SELECT ST_Y(ST_EndPoint(route_line::geometry)) AS lat,
                   ST_X(ST_EndPoint(route_line::geometry)) AS lon
            FROM walk_records
            WHERE id = :walkId AND route_line IS NOT NULL
            """, nativeQuery = true)
    Object[] getLastPoint(@Param("walkId") Long walkId);
}
