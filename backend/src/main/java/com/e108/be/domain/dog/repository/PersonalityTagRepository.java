package com.e108.be.domain.dog.repository;

import com.e108.be.domain.dog.entity.PersonalityTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalityTagRepository extends JpaRepository<PersonalityTag, Long> {

    Optional<PersonalityTag> findByTagName(String tagName);

    List<PersonalityTag> findByTagNameIn(List<String> tagNames);
}
