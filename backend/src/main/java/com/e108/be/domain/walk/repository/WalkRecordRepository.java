package com.e108.be.domain.walk.repository;

import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalkRecordRepository extends JpaRepository<WalkRecord, Long> {
    Optional<WalkRecord> findByDogIdAndWalkStatus(Long dogId, WalkStatus walkStatus);
}
