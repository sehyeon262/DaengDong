package com.e108.be.domain.route.service;

import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.route.dto.request.RouteSelectionRequest;
import com.e108.be.domain.route.entity.RouteSelectionLog;
import com.e108.be.domain.route.repository.RouteSelectionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 경로 선택 로그 서비스
 *
 * 사용자가 추천 경로를 선택할 때 호출되어 선택 데이터를 저장한다.
 * 축적된 데이터는 PlaceScoringService에서 개인화 가중치 학습에 활용된다.
 */
@Service
@RequiredArgsConstructor
public class RouteSelectionService {

    private final RouteSelectionLogRepository selectionLogRepository;
    private final DogRepository dogRepository;

    /**
     * 경로 선택 기록
     *
     * @param memberId 회원 ID
     * @param request  선택 정보 (경로 유형, 위치, 거리, 장소 수)
     */
    @Transactional
    public void logSelection(Long memberId, RouteSelectionRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Long dogId = dogRepository.findFirstByUser_Id(memberId)
                .map(dog -> dog.getId())
                .orElse(null);

        RouteSelectionLog log = RouteSelectionLog.builder()
                .memberId(memberId)
                .dogId(dogId)
                .selectedType(request.getSelectedType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .hourOfDay(now.getHour())
                .dayOfWeek(now.getDayOfWeek().getValue())
                .selectedDistanceM(request.getSelectedDistanceM())
                .selectedPlaceCount(request.getSelectedPlaceCount())
                .build();

        selectionLogRepository.save(log);
    }
}
