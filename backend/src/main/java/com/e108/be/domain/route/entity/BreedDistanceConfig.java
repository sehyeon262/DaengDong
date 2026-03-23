package com.e108.be.domain.route.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 견종/체중 구간별 적정 산책 거리 설정
 *
 * 체중 구간(소형/중형/대형)에 따른 권장 산책 거리를 정의한다.
 * Dog의 weight로 이 테이블을 조회하여 경로 거리 필터링에 활용한다.
 */
@Entity
@Table(name = "breed_distance_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BreedDistanceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "size_category", nullable = false, length = 10)
    private String sizeCategory;

    @Column(name = "min_weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal minWeightKg;

    @Column(name = "max_weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "recommended_distance_m", nullable = false)
    private int recommendedDistanceM;

    @Column(name = "max_distance_m", nullable = false)
    private int maxDistanceM;
}
