package com.e108.be.domain.place.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "place_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    // 경로 추천 알고리즘용 가산점 (예: 놀이터 +10, 쓰레기통 +2, 병원 0)
    @Column(name = "route_weight")
    private Integer routeWeight = 0;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Builder
    public PlaceCategory(String name, Integer routeWeight, String iconUrl) {
        this.name = name;
        this.routeWeight = routeWeight;
        this.iconUrl = iconUrl;
    }
}
