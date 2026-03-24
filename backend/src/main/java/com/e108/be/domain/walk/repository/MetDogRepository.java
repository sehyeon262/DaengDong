package com.e108.be.domain.walk.repository;

import com.e108.be.domain.walk.entity.MetDog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * 내 강아지가 만난 모든 강아지 목록 (최신순).
     * 만난 친구들 목록 조회 시 사용.
     */
    @Query("""
        SELECT m FROM MetDog m
        JOIN FETCH m.latestWalkRecord wr
        WHERE wr.dogId = :sourceDogId
        ORDER BY m.updatedAt DESC
        """)
    List<MetDog> findAllBySourceDogId(@Param("sourceDogId") Long sourceDogId);

    /**
     * 내 강아지(sourceDogId)가 비선호(싫어요)로 표시한 targetDogId 목록 조회.
     * 비선호 강아지 알림 판정 시 한 번에 조회하여 Set으로 활용.
     */
    @Query("""
        SELECT m.targetDogId FROM MetDog m
        JOIN m.latestWalkRecord wr
        WHERE wr.dogId = :sourceDogId
          AND m.feedback = com.e108.be.domain.walk.entity.Feedback.싫어요
        """)
    List<Long> findDislikedTargetDogIds(@Param("sourceDogId") Long sourceDogId);

    // ── 배지용 쿼리 ──

    @Query("SELECT COUNT(m) > 0 FROM MetDog m JOIN m.latestWalkRecord wr WHERE wr.dogId IN :dogIds")
    boolean existsMeetByDogIds(@Param("dogIds") List<Long> dogIds);

    @Query("SELECT COUNT(m) FROM MetDog m JOIN m.latestWalkRecord wr WHERE wr.dogId IN :dogIds")
    long countMetDogsByDogIds(@Param("dogIds") List<Long> dogIds);
}
