package com.e108.be.domain.route.entity;

import com.e108.be.domain.place.entity.PlaceCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 장소 카테고리 선호도
 *
 * 경로 선택 로그를 기반으로 집계된 카테고리별 선호 점수를 캐싱한다.
 * PlaceScoringService에서 전역 routeWeight 대신 개인화 가중치로 활용한다.
 */
@Entity
@Table(name = "user_category_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "category_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCategoryPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private PlaceCategory category;

    @Column(name = "selection_count", nullable = false)
    private int selectionCount;

    @Column(name = "preference_score", nullable = false)
    private double preferenceScore;

    @Builder
    public UserCategoryPreference(Long memberId, PlaceCategory category,
                                   int selectionCount, double preferenceScore) {
        this.memberId = memberId;
        this.category = category;
        this.selectionCount = selectionCount;
        this.preferenceScore = preferenceScore;
    }

    public void incrementCount() {
        this.selectionCount++;
    }

    public void updateScore(double score) {
        this.preferenceScore = score;
    }
}
