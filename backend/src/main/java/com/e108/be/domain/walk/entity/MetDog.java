package com.e108.be.domain.walk.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "met_dogs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetDog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // source dog은 latestWalkRecord.dogId로 식별 (ERD에 source_dog_id 컬럼 없음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_walk_record_id", nullable = false)
    private WalkRecord latestWalkRecord;

    @Column(name = "target_dog_id", nullable = false)
    private Long targetDogId;

    // 좋아요 | 보통 | 싫어요 (기본값: 보통)
    @Enumerated(EnumType.STRING)
    @Column(name = "feedback", nullable = false, length = 30)
    private Feedback feedback;

    @Builder
    public MetDog(WalkRecord latestWalkRecord, Long targetDogId, Feedback feedback) {
        this.latestWalkRecord = latestWalkRecord;
        this.targetDogId = targetDogId;
        this.feedback = feedback;
    }

    public void updateFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public void updateLatestWalkRecord(WalkRecord walkRecord) {
        this.latestWalkRecord = walkRecord;
    }
}
