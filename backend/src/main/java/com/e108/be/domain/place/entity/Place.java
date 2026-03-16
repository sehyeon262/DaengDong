package com.e108.be.domain.place.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * places 테이블 - 반려견 동반 가능 장소
 *
 * [공간 데이터 주의사항]
 * - location 컬럼은 geometry(Point, 4326) 타입 (경도/위도 WGS84 좌표계)
 * - Point 생성 시 반드시 X=경도(longitude), Y=위도(latitude) 순서
 * - 거리 계산 시 meters 단위를 원하면 ::geography 캐스팅 필요
 *   예: ST_DWithin(location::geography, ..., radiusMeters)
 */
@Entity
@Table(
    name = "places",
    uniqueConstraints = @UniqueConstraint(name = "uq_places_provider_source", columnNames = {"provider", "source_id"}),
    indexes = {
        // B-tree 인덱스 (JPA가 자동 생성)
        // GiST 공간 인덱스(location)는 PostGIS 전용이라 PlaceDbInitializer에서 별도 생성
        @Index(name = "idx_places_category_id", columnList = "category_id"),
        @Index(name = "idx_places_is_active",   columnList = "is_active")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 데이터 출처 (예: CULTURE_FACILITY, TOUR_API, USER)
    @Column(nullable = false, length = 50)
    private String provider;

    // 출처 내 고유 번호 (중복 적재 방지용)
    @Column(name = "source_id", nullable = false, length = 100)
    private String sourceId;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private PlaceCategory category;

    // geometry(Point, 4326): X=경도, Y=위도
    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point location;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String contact;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 폐업/삭제 여부 (Soft Delete)
    // columnDefinition으로 DB 레벨 DEFAULT 지정 → INSERT 시 컬럼 생략해도 NULL 오류 없음
    @Column(name = "is_active", nullable = false, columnDefinition = "boolean DEFAULT true")
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Place(String provider, String sourceId, String name, PlaceCategory category,
                 Point location, String address, String contact, String imageUrl, String description) {
        this.provider = provider;
        this.sourceId = sourceId;
        this.name = name;
        this.category = category;
        this.location = location;
        this.address = address;
        this.contact = contact;
        this.imageUrl = imageUrl;
        this.description = description;
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
