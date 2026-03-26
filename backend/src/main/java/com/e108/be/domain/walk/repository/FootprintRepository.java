package com.e108.be.domain.walk.repository;

import com.e108.be.domain.walk.entity.Footprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FootprintRepository extends JpaRepository<Footprint, Long> {

    // 같은 산책 + 같은 장소 중복 도장 방지
    boolean existsByWalkRecordIdAndPlaceId(Long walkRecordId, Long placeId);

    // 강아지가 도장 찍은 장소 ID 목록 (중복 제거)
    @Query("SELECT DISTINCT f.placeId FROM Footprint f WHERE f.dogId = :dogId AND f.placeId IS NOT NULL")
    List<Long> findDistinctPlaceIdsByDogId(@Param("dogId") Long dogId);

    // 강아지들의 총 발자국 도장 수
    @Query("SELECT COUNT(f) FROM Footprint f WHERE f.dogId IN :dogIds")
    long countByDogIds(@Param("dogIds") List<Long> dogIds);
}
