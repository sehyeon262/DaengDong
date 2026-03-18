package com.e108.be.domain.dog.entity;

import com.e108.be.domain.auth.entity.User;
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
import java.util.stream.Collectors;

@Entity
@Table(name = "dogs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String breed;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(length = 20)
    private String gender;

    @Column(name = "neutered_yn")
    private Boolean neuteredYn;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "dog_personality_tags",
            joinColumns = @JoinColumn(name = "dog_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<PersonalityTag> personalityTags = new ArrayList<>();

    @Builder
    public Dog(User user, String name, String breed, LocalDate birthDate,
               BigDecimal weight, String gender, Boolean neuteredYn, String profileImageUrl) {
        this.user = user;
        this.name = name;
        this.breed = breed;
        this.birthDate = birthDate;
        this.weight = weight;
        this.gender = gender;
        this.neuteredYn = neuteredYn;
        this.profileImageUrl = profileImageUrl;
    }

    public List<String> getTraitNames() {
        return personalityTags.stream()
                .map(PersonalityTag::getTagName)
                .collect(Collectors.toList());
    }

    public void updateProfile(String name, String breed, LocalDate birthDate, String gender) {
        if (name != null) this.name = name;
        if (breed != null) this.breed = breed;
        if (birthDate != null) this.birthDate = birthDate;
        if (gender != null) this.gender = gender;
    }

    public void updateWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public void updateTraits(List<PersonalityTag> newTags) {
        this.personalityTags.clear();
        this.personalityTags.addAll(newTags);
    }
}
