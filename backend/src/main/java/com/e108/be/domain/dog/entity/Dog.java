package com.e108.be.domain.dog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * [dogs 테이블 매핑 - 읽기 전용]
 *
 * 칼로리 계산에 필요한 체중(weight) 조회 목적으로만 사용
 * Dog 도메인 전체 구현은 담당 팀원이 별도 진행
 */
@Entity
@Table(name = "dogs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 체중 (단위: kg) - 칼로리 계산에 사용
    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;
}
