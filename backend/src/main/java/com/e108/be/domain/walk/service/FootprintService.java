package com.e108.be.domain.walk.service;

import com.e108.be.domain.walk.entity.Footprint;
import com.e108.be.domain.walk.repository.FootprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발자국 도장 서비스
 * 산책 중 사용자가 장소 근처에서 직접 터치 시 도장 1개 등록
 */
@Service
@RequiredArgsConstructor
public class FootprintService {

    private final FootprintRepository footprintRepository;

    /**
     * 발자국 도장 1개 등록
     * FE에서 50m 이내 장소 감지 → 사용자 터치 → 호출
     * 같은 산책 + 장소 조합은 중복 방지
     *
     * @param walkId  현재 산책 ID
     * @param dogId   강아지 ID
     * @param placeId 방문한 장소 ID
     */
    @Transactional
    public void cancelStamp(Long dogId, Long placeId) {
        footprintRepository.deleteByDogIdAndPlaceId(dogId, placeId);
    }

    @Transactional
    public void registerStamp(Long walkId, Long dogId, Long placeId) {
        if (footprintRepository.existsByWalkRecordIdAndPlaceId(walkId, placeId)) {
            return;
        }
        footprintRepository.save(
                Footprint.builder()
                        .dogId(dogId)
                        .walkRecordId(walkId)
                        .placeId(placeId)
                        .build()
        );
    }
}
