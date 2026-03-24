package com.e108.be.domain.badge.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "badges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Badge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "badge_name", nullable = false, length = 100, unique = true)
    private String badgeName;

    @Column(name = "condition_value", length = 100)
    private String conditionValue;

    @Column(length = 255)
    private String description;
}
