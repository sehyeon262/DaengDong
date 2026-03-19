package com.e108.be.domain.diary.service;

import com.e108.be.domain.diary.dto.response.DiaryResponse;
import com.e108.be.domain.diary.entity.Diary;
import com.e108.be.domain.diary.exception.DiaryNotFoundException;
import com.e108.be.domain.diary.repository.DiaryRepository;
import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import com.e108.be.domain.walk.exception.WalkNotFoundException;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private static final double MIN_DISTANCE_METERS = 300.0;

    private final DiaryRepository diaryRepository;
    private final WalkRecordRepository walkRecordRepository;
    private final DogRepository dogRepository;
    private final DiaryGenerationWorker diaryGenerationWorker;

    @Transactional
    public void createDiaryIfEligible(Long walkId, Long dogId, BigDecimal totalDistance) {
        if (totalDistance == null || totalDistance.doubleValue() < MIN_DISTANCE_METERS) {
            return;
        }
        if (diaryRepository.existsByWalkId(walkId)) {
            return;
        }

        Diary diary = Diary.builder()
                .walkId(walkId)
                .dogId(dogId)
                .build();
        diaryRepository.save(diary);

        diaryGenerationWorker.generate(diary.getId(), walkId, dogId);
    }

    /**
     * 기존 COMPLETED 산책에 대해 수동으로 일기 생성
     */
    @Transactional
    public void generateForExistingWalk(Long walkId) {
        WalkRecord walk = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        if (walk.getWalkStatus() != WalkStatus.COMPLETED) {
            throw new IllegalArgumentException("완료된 산책만 일기를 생성할 수 있습니다.");
        }

        // 이미 완성된 일기가 있으면 스킵, 실패한 일기(content=null)는 삭제 후 재생성
        Diary existing = diaryRepository.findByWalkId(walkId).orElse(null);
        if (existing != null) {
            if (existing.getContent() != null) {
                return; // 이미 완성된 일기
            }
            diaryRepository.delete(existing); // 실패한 일기 삭제
        }

        Diary diary = Diary.builder()
                .walkId(walkId)
                .dogId(walk.getDogId())
                .build();
        diaryRepository.save(diary);

        diaryGenerationWorker.generate(diary.getId(), walkId, walk.getDogId());
    }

    public DiaryResponse getDiary(Long walkId) {
        WalkRecord walk = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);
        Dog dog = dogRepository.findById(walk.getDogId())
                .orElseThrow(WalkNotFoundException::new);
        Diary diary = diaryRepository.findByWalkId(walkId)
                .orElseThrow(DiaryNotFoundException::new);
        return DiaryResponse.from(diary, walk, dog);
    }
}
