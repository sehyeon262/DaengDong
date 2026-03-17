package com.e108.be.domain.place.repository;

import com.e108.be.domain.place.entity.PlaceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceCategoryRepository extends JpaRepository<PlaceCategory, Integer> {

    Optional<PlaceCategory> findByName(String name);
}
