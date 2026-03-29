package com.e108.be.domain.safety.repository;

import com.e108.be.domain.safety.entity.RiskReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * [RiskReport JPA Repository]
 *
 * 기본 CRUD는 JpaRepository가 제공.
 *
 * ※ Hibernate Spatial(hibernate-spatial 의존성)이 정상 활성화된 경우,
 *   Point 필드는 JPA save()로 geography(POINT, 4326) 컬럼에 자동 매핑됩니다.
 *
 * ※ 만약 Hibernate Spatial 매핑이 정상 동작하지 않는 경우(예: SRID 오류, 컬럼 타입 불일치),
 *   아래 saveWithNativePoint() 네이티브 쿼리를 대안으로 사용하세요.
 *   이 경우 RiskReport 엔티티의 location 필드를 @Transient 로 변경하고
 *   서비스에서 save() 대신 saveWithNativePoint() 를 호출합니다.
 */
public interface RiskReportRepository extends JpaRepository<RiskReport, Long> {

    Optional<RiskReport> findByIdAndUserId(Long id, Long userId);

    /**
     * Projection interface for nearby risk report query results with distance
     */
    interface NearbyRiskReportProjection {
        Long getRiskReportId();
        Long getWalkSessionId();
        Double getLatitude();
        Double getLongitude();
        String getDescription();
        java.time.LocalDateTime getCreatedAt();
        Double getDistanceM();
    }

    /**
     * [대안] PostGIS ST_SetSRID + ST_MakePoint 를 사용한 네이티브 INSERT
     *
     * Hibernate Spatial 매핑이 불가한 경우에만 활성화하여 사용.
     * - longitude, latitude 를 받아 POINT(경도 위도) 로 변환 후 저장
     * - 반환 타입이 void이므로 저장 후 ID가 필요하면 별도 조회 필요
     *
     * 사용 시 주의:
     * 1. 이 쿼리를 사용할 경우 RiskReport.location 필드에 @Transient 를 추가하세요.
     * 2. createdAt, updatedAt 은 DB DEFAULT(now()) 또는 트리거로 처리해야 합니다.
     *
     * @param userId        신고한 사용자 ID
     * @param walkSessionId 산책 세션 ID (nullable)
     * @param longitude     경도 (x축)
     * @param latitude      위도 (y축)
     * @param description   위험 요소 설명
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO risk_reports (user_id, walk_session_id, location, description)
            VALUES (:userId, :walkSessionId,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :description)
            """,
        nativeQuery = true
    )
    void saveWithNativePoint(
            @Param("userId") Long userId,
            @Param("walkSessionId") Long walkSessionId,
            @Param("longitude") double longitude,
            @Param("latitude") double latitude,
            @Param("description") String description
    );

    long countByUserId(Long userId);

    /**
     * 현재 위치 기준 반경 내 위험 구역 목록 조회 (전체 사용자)
     *
     * ST_DWithin(geography, geography, meters) — PostGIS 지리 거리 필터
     */
    @Query(value = """
            SELECT * FROM risk_reports
            WHERE ST_DWithin(
                location::geography,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                :radiusMeters
            )
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<RiskReport> findWithinRadius(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters
    );

    /**
     * 특정 사용자의 위험 구역 전체 목록 조회
     *
     * 앱 재실행 시 개인 위험장소 복원 용도
     */
    List<RiskReport> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 사용자의 위험 구역 중 반경 내 목록 조회 (거리 포함)
     *
     * ST_DWithin — 반경 필터
     * ST_Distance — 실제 거리 계산 (미터 단위)
     *
     * 산책 중 근접 알림 입력 용도
     */
    @Query(value = """
            SELECT
                r.risk_report_id AS riskReportId,
                r.walk_session_id AS walkSessionId,
                ST_Y(r.location::geometry) AS latitude,
                ST_X(r.location::geometry) AS longitude,
                r.description AS description,
                r.created_at AS createdAt,
                ST_Distance(
                    r.location::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                ) AS distanceM
            FROM risk_reports r
            WHERE r.user_id = :userId
              AND ST_DWithin(
                    r.location::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :radiusMeters
              )
            ORDER BY distanceM ASC
            """, nativeQuery = true)
    List<NearbyRiskReportProjection> findByUserIdWithinRadius(
            @Param("userId") Long userId,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters
    );
}
