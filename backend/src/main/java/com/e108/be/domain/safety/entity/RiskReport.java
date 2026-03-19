package com.e108.be.domain.safety.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

/**
 * [risk_reports 테이블 매핑]
 *
 * 사용자가 산책 중 신고한 위험 구역 정보를 담는 엔티티
 *
 * location:
 *   - 타입: GEOGRAPHY(POINT, 4326) - PostGIS
 *   - POINT(경도 위도) 형식으로 저장 (x=longitude, y=latitude)
 */
@Entity
@Table(name = "risk_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "risk_report_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // walk_session_id — nullable (산책 중이 아닐 때도 신고 가능)
    @Column(name = "walk_session_id")
    private Long walkSessionId;

    // PostGIS GEOGRAPHY(POINT, 4326) — Hibernate Spatial(JTS) 매핑
    @Column(name = "location", columnDefinition = "geography(POINT, 4326)", nullable = false)
    private Point location;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Builder
    public RiskReport(Long userId, Long walkSessionId, Point location, String description) {
        this.userId = userId;
        this.walkSessionId = walkSessionId;
        this.location = location;
        this.description = description;
    }
}
