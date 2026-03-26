package com.e108.be.domain.route.entity;

import com.e108.be.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경로 선택 시 포함된 장소 (로그-장소 연결 테이블)
 *
 * 사용자가 선택한 경로에 포함된 장소들을 방문 순서와 함께 기록한다.
 * 이 데이터로 카테고리별 선호도를 집계할 수 있다.
 */
@Entity
@Table(name = "route_selection_places", indexes = {
        @Index(name = "idx_rsp_selection_log_id", columnList = "selection_log_id"),
        @Index(name = "idx_rsp_place_id", columnList = "place_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteSelectionPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selection_log_id", nullable = false)
    private RouteSelectionLog selectionLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "visit_order", nullable = false)
    private int visitOrder;

    @Builder
    public RouteSelectionPlace(Place place, int visitOrder) {
        this.place = place;
        this.visitOrder = visitOrder;
    }

    void assignLog(RouteSelectionLog log) {
        this.selectionLog = log;
    }
}
