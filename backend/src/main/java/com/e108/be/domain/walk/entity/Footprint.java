package com.e108.be.domain.walk.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [footprints 테이블 매핑]
 * 산책 중 발생한 발자국(이벤트) 기록
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

    // ERD의 id2 컬럼 - 산책 세션 ID
    @Column(name = "id2")
    private Long walkRecordId;
}
