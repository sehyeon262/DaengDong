package com.e108.be.domain.diary.repository;

import com.e108.be.domain.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    Optional<Diary> findByWalkId(Long walkId);

    boolean existsByWalkId(Long walkId);
}
