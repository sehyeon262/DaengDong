package com.e108.be.domain.route.entity;

import com.e108.be.domain.route.dto.response.RouteType;
import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경로 선택 로그
 *
 * 사용자가 추천된 3개 경로 중 어떤 경로를 선택했는지 기록한다.
 * 이 데이터를 기반으로 스코어링 가중치를 학습하여
 * 개인화된 경로 추천을 제공한다.
 */
@Entity
@Table(name = "route_selection_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteSelectionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "dog_id")
    private Long dogId;

    // 선택된 경로 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "selected_type", nullable = false, length = 20)
    private RouteType selectedType;

    // 요청 시 위치
    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    // 컨텍스트 정보 (패턴 분석용)
    @Column(name = "hour_of_day")
    private int hourOfDay;

    @Column(name = "day_of_week")
    private int dayOfWeek;

    // 선택된 경로의 총 거리 (미터)
    @Column(name = "selected_distance_m")
    private int selectedDistanceM;

    // 선택된 경로의 장소 수
    @Column(name = "selected_place_count")
    private int selectedPlaceCount;

    @Builder
    public RouteSelectionLog(Long memberId, Long dogId, RouteType selectedType,
                              double latitude, double longitude,
                              int hourOfDay, int dayOfWeek,
                              int selectedDistanceM, int selectedPlaceCount) {
        this.memberId = memberId;
        this.dogId = dogId;
        this.selectedType = selectedType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hourOfDay = hourOfDay;
        this.dayOfWeek = dayOfWeek;
        this.selectedDistanceM = selectedDistanceM;
        this.selectedPlaceCount = selectedPlaceCount;
    }
}
