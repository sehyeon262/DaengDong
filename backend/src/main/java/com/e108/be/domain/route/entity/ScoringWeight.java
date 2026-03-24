package com.e108.be.domain.route.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학습된 스코어링 가중치
 *
 * PlaceScoringService의 하드코딩 가중치(0.5, 0.5)를
 * 사용자 선택 데이터 기반으로 학습한 값으로 대체한다.
 *
 * ScoringWeightLearner가 주기적으로 학습하여 갱신한다.
 * 데이터 부족 시 기존 기본값을 사용한다.
 */
@Entity
@Table(name = "scoring_weights")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScoringWeight extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 피처 이름 (DISTANCE, CATEGORY)
     */
    @Column(name = "feature_name", nullable = false, unique = true, length = 30)
    private String featureName;

    /**
     * 학습된 가중치 (0.0 ~ 1.0)
     */
    @Column(name = "weight", nullable = false)
    private double weight;

    /**
     * 학습에 사용된 데이터 건수
     */
    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Builder
    public ScoringWeight(String featureName, double weight, int sampleCount) {
        this.featureName = featureName;
        this.weight = weight;
        this.sampleCount = sampleCount;
    }

    public void updateWeight(double weight, int sampleCount) {
        this.weight = weight;
        this.sampleCount = sampleCount;
    }
}
