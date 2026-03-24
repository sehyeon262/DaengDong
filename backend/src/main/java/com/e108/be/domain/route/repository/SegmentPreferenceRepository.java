package com.e108.be.domain.route.repository;

import com.e108.be.domain.route.entity.SegmentPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SegmentPreferenceRepository extends JpaRepository<SegmentPreference, Long> {

    List<SegmentPreference> findByWeightGroup(String weightGroup);
}
