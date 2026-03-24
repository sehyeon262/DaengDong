package com.e108.be.domain.diary.entity;

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

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

    /** 대표 감정 태그 (즐거움, 평온, 슬픔, 불안) - 가장 높은 신뢰도 기준 */
    @Column(name = "emotion_tag", length = 20)
    private String emotionTag;

    /** 사진별 감정 태그 - { "https://...photo1.jpg": "즐거움", "https://...photo2.jpg": "슬픔" } */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "photo_emotions", columnDefinition = "json")
    private Map<String, String> photoEmotions;

    @Builder
    public Diary(Long walkId, Long dogId) {
        this.walkId = walkId;
        this.dogId = dogId;
        this.content = null;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateEmotionTag(String emotionTag) {
        this.emotionTag = emotionTag;
    }

    public void updatePhotoEmotions(Map<String, String> photoEmotions) {
        this.photoEmotions = photoEmotions;
    }
}
