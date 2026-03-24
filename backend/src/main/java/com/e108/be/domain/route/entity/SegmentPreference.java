package com.e108.be.domain.route.entity;

import com.e108.be.domain.place.entity.PlaceCategory;
import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세그먼트(체중 구간)별 카테고리 선호도
 *
 * 같은 체중 구간의 반려견을 키우는 사용자들의 카테고리 선호도를 집계한다.
 * 개인 데이터가 부족한 신규 사용자(콜드스타트)에게
 * 같은 세그먼트의 선호도를 적용하여 추천 품질을 높인다.
 *
 * 세그먼트: 반려견 체중 구간 (SMALL / MEDIUM / LARGE)
 * - SMALL:  0 ~ 10kg
 * - MEDIUM: 10 ~ 25kg
 * - LARGE:  25kg 이상
 */
@Entity
@Table(name = "segment_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_segment_category",
                columnNames = {"weight_group", "category_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SegmentPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 체중 그룹 (SMALL, MEDIUM, LARGE)
     */
    @Column(name = "weight_group", nullable = false, length = 10)
    private String weightGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private PlaceCategory category;

    /**
     * 정규화된 선호도 점수 (0.0 ~ 1.0)
     */
    @Column(name = "preference_score", nullable = false)
    private double preferenceScore;

    /**
     * 집계에 참여한 사용자 수
     */
    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Builder
    public SegmentPreference(String weightGroup, PlaceCategory category,
                              double preferenceScore, int sampleCount) {
        this.weightGroup = weightGroup;
        this.category = category;
        this.preferenceScore = preferenceScore;
        this.sampleCount = sampleCount;
    }

    public void updatePreference(double preferenceScore, int sampleCount) {
        this.preferenceScore = preferenceScore;
        this.sampleCount = sampleCount;
    }
}
