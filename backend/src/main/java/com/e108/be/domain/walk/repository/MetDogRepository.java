package com.e108.be.domain.walk.repository;

import com.e108.be.domain.walk.entity.MetDog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MetDogRepository extends JpaRepository<MetDog, Long> {

    /**
     * source dog(내 강아지)과 target dog 조합으로 기존 met_dog 레코드 조회.
     * source dog은 latestWalkRecord.dogId로 식별.
     * upsert 및 feedback 업데이트 시 사용.
     */
    @Query("""
        SELECT m FROM MetDog m
        JOIN m.latestWalkRecord wr
        WHERE wr.dogId = :sourceDogId
          AND m.targetDogId = :targetDogId
        """)
    Optional<MetDog> findBySourceDogIdAndTargetDogId(
            @Param("sourceDogId") Long sourceDogId,
            @Param("targetDogId") Long targetDogId
    );
}
