package com.e108.be.domain.walk.repository;

import com.e108.be.domain.walk.entity.Footprint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FootprintRepository extends JpaRepository<Footprint, Long> {

    // 같은 산책 + 같은 장소 중복 도장 방지
    boolean existsByWalkRecordIdAndPlaceId(Long walkRecordId, Long placeId);
}
