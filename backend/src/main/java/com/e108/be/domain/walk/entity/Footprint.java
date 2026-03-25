package com.e108.be.domain.walk.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [footprints 테이블 매핑]
 * 산책 중 방문한 장소 발자국 기록
 * - place_id: 방문한 장소 (산책 종료 시 route_line 기준 자동 등록)
 */
@Entity
@Table(name = "footprints")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Footprint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dog_id", nullable = false)
    private Long dogId;

    @Column(name = "walk_record_id", nullable = false)
    private Long walkRecordId;

    // 방문한 장소 ID (NULL 허용)
    @Column(name = "place_id")
    private Long placeId;

    @Builder
    public Footprint(Long dogId, Long walkRecordId, Long placeId) {
        this.dogId = dogId;
        this.walkRecordId = walkRecordId;
        this.placeId = placeId;
    }
}
