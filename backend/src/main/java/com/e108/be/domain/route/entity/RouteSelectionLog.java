package com.e108.be.domain.route.entity;

import com.e108.be.domain.route.dto.response.RouteType;
import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 경로 선택 로그
 *
 * 사용자가 추천된 경로 중 어떤 경로를 선택했는지 기록한다.
 * 이 데이터를 기반으로 스코어링 가중치를 학습하여
 * 개인화된 경로 추천을 제공한다.
 */
@Entity
@Table(name = "route_selection_logs", indexes = {
        @Index(name = "idx_rsl_user_weather", columnList = "user_id, weather_condition")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteSelectionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "dog_id")
    private Long dogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_type", nullable = false, length = 20)
    private RouteType selectedType;

    @Column(name = "selected_distance_m")
    private int selectedDistanceM;

    @Column(name = "hour_of_day")
    private int hourOfDay;

    @Column(name = "day_of_week")
    private int dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_condition", length = 20)
    private WeatherCondition weatherCondition;

    @Column(name = "temperature")
    private Double temperature;

    @OneToMany(mappedBy = "selectionLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteSelectionPlace> selectedPlaces = new ArrayList<>();

    @Builder
    public RouteSelectionLog(Long userId, Long dogId, RouteType selectedType,
                              int selectedDistanceM,
                              int hourOfDay, int dayOfWeek,
                              WeatherCondition weatherCondition, Double temperature) {
        this.userId = userId;
        this.dogId = dogId;
        this.selectedType = selectedType;
        this.selectedDistanceM = selectedDistanceM;
        this.hourOfDay = hourOfDay;
        this.dayOfWeek = dayOfWeek;
        this.weatherCondition = weatherCondition;
        this.temperature = temperature;
    }

    public void addPlace(RouteSelectionPlace place) {
        this.selectedPlaces.add(place);
        place.assignLog(this);
    }
}
