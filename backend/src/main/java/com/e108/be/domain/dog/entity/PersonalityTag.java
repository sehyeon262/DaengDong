package com.e108.be.domain.dog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "personality_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalityTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tag_name", unique = true, nullable = false, length = 100)
    private String tagName;

    @Builder
    public PersonalityTag(String tagName) {
        this.tagName = tagName;
    }
}
