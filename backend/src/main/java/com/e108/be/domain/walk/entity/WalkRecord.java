package com.e108.be.domain.walk.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(name = "total_distance", precision = 10, scale = 2)
    private BigDecimal totalDistance;

    @Column(name = "total_duration")
    private Integer totalDuration;

    @Column(name = "route_line")
    private String routeLine;

    @Column(name = "calories", precision = 10, scale = 2)
    private BigDecimal calories;

    @Builder
    public WalkRecord(Long dogId, WalkStatus walkStatus, LocalDateTime startTime) {
        this.dogId = dogId;
        this.walkStatus = walkStatus;
        this.startTime = startTime;
    }
}
