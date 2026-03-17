package com.e108.be.domain.dog.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
public class Dog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dog_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String breed;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    // 체중 (단위: kg) - 칼로리 계산에 사용
    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(length = 20)
    private String gender;

    @Column(name = "neutered_yn")
    private Boolean neuteredYn;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    // 별도 dog_traits 테이블로 관리 (ERD에 없지만 API 명세 요구사항)
    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "dog_traits", joinColumns = @JoinColumn(name = "dog_id"))
    @Column(name = "trait")
    private List<String> traits = new ArrayList<>();

    @Builder
    public Dog(Long userId, String name, String breed, LocalDate birthDate,
               BigDecimal weight, String gender, Boolean neuteredYn, String profileImageUrl) {
        this.userId = userId;
        this.name = name;
        this.breed = breed;
        this.birthDate = birthDate;
        this.weight = weight;
        this.gender = gender;
        this.neuteredYn = neuteredYn;
        this.profileImageUrl = profileImageUrl;
    }

    // 프로필 부분 수정 (null이면 기존 값 유지)
    public void updateProfile(String name, String breed, LocalDate birthDate, String gender) {
        if (name != null) this.name = name;
        if (breed != null) this.breed = breed;
        if (birthDate != null) this.birthDate = birthDate;
        if (gender != null) this.gender = gender;
    }

    // 체중 수정
    public void updateWeight(BigDecimal weight) {
        this.weight = weight;
    }

    // 성향 태그 전체 교체
    public void updateTraits(List<String> newTraits) {
        this.traits.clear();
        this.traits.addAll(newTraits);
    }
}
