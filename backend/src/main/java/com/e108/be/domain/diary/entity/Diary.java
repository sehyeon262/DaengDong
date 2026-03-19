package com.e108.be.domain.diary.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "diaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private Long id;

    @Column(name = "walk_id", nullable = false, unique = true)
    private Long walkId;

    @Column(name = "dog_id", nullable = false)
    private Long dogId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder
    public Diary(Long walkId, Long dogId) {
        this.walkId = walkId;
        this.dogId = dogId;
        this.content = null;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
