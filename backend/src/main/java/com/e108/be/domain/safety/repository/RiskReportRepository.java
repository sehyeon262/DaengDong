package com.e108.be.domain.safety.repository;

import com.e108.be.domain.safety.entity.RiskReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
