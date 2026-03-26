package com.e108.be.domain.route.repository;

import com.e108.be.domain.route.entity.ScoringWeight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScoringWeightRepository extends JpaRepository<ScoringWeight, Long> {

    Optional<ScoringWeight> findByFeatureName(String featureName);
}
