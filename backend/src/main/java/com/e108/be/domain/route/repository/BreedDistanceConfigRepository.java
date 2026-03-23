package com.e108.be.domain.route.repository;

import com.e108.be.domain.route.entity.BreedDistanceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface BreedDistanceConfigRepository extends JpaRepository<BreedDistanceConfig, Integer> {

    /**
     * 체중으로 해당 구간의 적정 거리 설정 조회
     */
    @Query("""
        SELECT b FROM BreedDistanceConfig b
        WHERE b.minWeightKg <= :weight AND b.maxWeightKg >= :weight
        """)
    Optional<BreedDistanceConfig> findByWeight(@Param("weight") BigDecimal weight);
}
