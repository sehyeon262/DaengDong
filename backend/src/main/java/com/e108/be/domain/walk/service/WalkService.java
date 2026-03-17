package com.e108.be.domain.walk.service;

import com.e108.be.domain.walk.dto.request.StartWalkRequest;
import com.e108.be.domain.walk.dto.response.EndWalkResponse;
import com.e108.be.domain.walk.dto.response.StartWalkResponse;
import com.e108.be.domain.walk.dto.response.WalkDurationResponse;
import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import com.e108.be.domain.walk.exception.WalkAlreadyEndedException;
import com.e108.be.domain.walk.exception.WalkAlreadyInProgressException;
import com.e108.be.domain.walk.exception.WalkNotFoundException;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkService {

    private final WalkRecordRepository walkRecordRepository;

    @Transactional
    public StartWalkResponse startWalk(StartWalkRequest request) {
        // 이미 진행 중인 산책이 있는지 확인
        walkRecordRepository.findByDogIdAndWalkStatus(request.getDogId(), WalkStatus.IN_PROGRESS)
                .ifPresent(w -> { throw new WalkAlreadyInProgressException(); });

        WalkRecord walkRecord = WalkRecord.builder()
                .dogId(request.getDogId())
                .walkStatus(WalkStatus.IN_PROGRESS)
                .startTime(LocalDateTime.now())
                .build();

        WalkRecord saved = walkRecordRepository.save(walkRecord);
        return StartWalkResponse.from(saved);
    }

    /**
     * W1-03 산책 시간 조회
     * GET /api/v1/walks/{walkId}/duration
     */
    public WalkDurationResponse getWalkDuration(Long walkId) {
        WalkRecord walkRecord = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);
        return WalkDurationResponse.from(walkRecord);
    }

    /**
     * W1-06 산책 종료
     * POST /api/v1/walks/{walkId}/end
     */
    @Transactional
    public EndWalkResponse endWalk(Long walkId) {
        WalkRecord walkRecord = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        // 이미 종료된 산책인지 확인
        if (walkRecord.getWalkStatus() != WalkStatus.IN_PROGRESS) {
            throw new WalkAlreadyEndedException();
        }

        // TODO: GPS 연동 완료 후 footprintService 등에서 실제 거리·칼로리를 계산해 전달
        BigDecimal totalDistance = BigDecimal.ZERO;
        BigDecimal calories = BigDecimal.ZERO;

        walkRecord.end(totalDistance, calories);
        return EndWalkResponse.from(walkRecord);
    }
}
