package com.e108.be.domain.route.repository;

import com.e108.be.domain.route.entity.SegmentPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SegmentPreferenceRepository extends JpaRepository<SegmentPreference, Long> {

    /**
     * 체중 × 연령 정확 매칭
     */
    List<SegmentPreference> findByWeightGroupAndAgeGroup(String weightGroup, String ageGroup);

    /**
     * 체중만 매칭 (2차원 매칭 데이터 부족 시 폴백)
     */
    List<SegmentPreference> findByWeightGroup(String weightGroup);
}
