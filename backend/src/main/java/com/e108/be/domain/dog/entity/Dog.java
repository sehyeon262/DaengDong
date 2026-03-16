package com.e108.be.domain.dog.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "dogs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String breed;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(length = 20)
    private String gender;

    @Column(name = "neutered_yn")
    private Boolean neuteredYn;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

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
}
