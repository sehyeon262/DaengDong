package com.e108.be.domain.walk.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * [walk_records 테이블 매핑]
 *
 * 산책 세션 1건의 전체 정보를 담는 엔티티
 *
 * route_line:
 *   - 타입: GEOGRAPHY(LINESTRING, 4326) - PostGIS
 *   - GPS 좌표를 순서대로 이은 선으로 저장
 *   - 산책 종료 시 Redis에 쌓인 좌표들로 생성
 *
 * total_distance: 미터(m) 단위
 * calories: 소모 칼로리 (kcal)
 */
@Entity
@Table(name = "walk_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalkRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dog 도메인 구현 전 임시 Long 참조 (Dog 도메인 완성 후 @ManyToOne 으로 교체)
    @Column(name = "dog_id", nullable = false)
    private Long dogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "walk_status", length = 30)
    private WalkStatus walkStatus;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // 총 이동 거리 (단위: m)
    @Column(name = "total_distance", precision = 10, scale = 2)
    private BigDecimal totalDistance;

    // 총 산책 시간 (단위: 초)
    @Column(name = "total_duration")
    private Integer totalDuration;

    // GPS 경로 선 - PostGIS LINESTRING 타입
    @Column(name = "route_line", columnDefinition = "geography(LINESTRING, 4326)")
    private LineString routeLine;

    // 소모 칼로리 (단위: kcal)
    @Column(name = "calories", precision = 10, scale = 2)
    private BigDecimal calories;

    @Builder
    public WalkRecord(Long dogId, WalkStatus walkStatus, LocalDateTime startTime) {
        this.dogId = dogId;
        this.walkStatus = walkStatus;
        this.startTime = startTime;
    }

    /**
     * 산책 종료 처리
     * - 종료 시각, 총 시간(초) 자동 계산
     * - totalDistance, calories는 외부에서 주입 (GPS 연동 완료 후 실제 값 전달)
     * - GPS 연동 전: BigDecimal.ZERO 전달
     *
     * @param totalDistance 총 이동 거리 (km) - GPS 연동 후 실제 값으로 전달
     * @param calories      소모 칼로리 - GPS 연동 후 실제 값으로 전달
     */
    public void end(BigDecimal totalDistance, BigDecimal calories) {
        LocalDateTime now = LocalDateTime.now();
        this.endTime = now;
        this.totalDuration = (int) ChronoUnit.SECONDS.between(this.startTime, now);
        this.totalDistance = totalDistance;
        this.calories = calories;
        this.walkStatus = WalkStatus.COMPLETED;
    }

    public void complete(LocalDateTime endTime, BigDecimal totalDistance,
                         Integer totalDuration, LineString routeLine, BigDecimal calories) {
        this.endTime = endTime;
        this.totalDistance = totalDistance;
        this.routeLine = routeLine;
        this.calories = calories;
        this.totalDuration = totalDuration;
        this.walkStatus = WalkStatus.COMPLETED;
    }

    public void updateDistance(BigDecimal totalDistance) {
        this.totalDistance = totalDistance;
    }

    public void updateCalories(BigDecimal calories) {
        this.calories = calories;
    }
}
