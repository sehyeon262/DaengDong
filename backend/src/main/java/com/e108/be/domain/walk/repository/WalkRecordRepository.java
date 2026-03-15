package com.e108.be.domain.walk.repository;

import com.e108.be.domain.walk.entity.WalkRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalkRecordRepository extends JpaRepository<WalkRecord, Long> {
}
