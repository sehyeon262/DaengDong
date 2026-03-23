package com.e108.be.domain.route.service;

import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.place.entity.Place;
import com.e108.be.domain.place.repository.PlaceRepository;
import com.e108.be.domain.route.dto.request.RouteSelectionRequest;
import com.e108.be.domain.route.entity.RouteSelectionLog;
import com.e108.be.domain.route.entity.RouteSelectionPlace;
import com.e108.be.domain.route.entity.UserCategoryPreference;
import com.e108.be.domain.route.repository.RouteSelectionLogRepository;
import com.e108.be.domain.route.repository.UserCategoryPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 경로 선택 로그 서비스
 *
 * 사용자가 추천 경로를 선택할 때 호출되어 선택 데이터를 저장하고
 * 카테고리별 선호도를 갱신한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteSelectionService {

    private final RouteSelectionLogRepository selectionLogRepository;
    private final UserCategoryPreferenceRepository preferenceRepository;
    private final PlaceRepository placeRepository;
    private final DogRepository dogRepository;

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
                .selectedDistanceM(request.getSelectedDistanceM())
                .hourOfDay(now.getHour())
                .dayOfWeek(now.getDayOfWeek().getValue())
                .weatherCondition(request.getWeatherCondition())
                .temperature(request.getTemperature())
                .build();

        // 선택된 장소 연결
        if (request.getPlaceIds() != null) {
            List<Place> places = placeRepository.findAllById(request.getPlaceIds());
            for (int i = 0; i < places.size(); i++) {
                RouteSelectionPlace selectionPlace = RouteSelectionPlace.builder()
                        .place(places.get(i))
                        .visitOrder(i + 1)
                        .build();
                log.addPlace(selectionPlace);
            }
        }

        selectionLogRepository.save(log);

        // 카테고리별 선호도 갱신
        updateCategoryPreferences(memberId, log.getSelectedPlaces());
    }

    /**
     * 선택된 장소의 카테고리별 선호도를 갱신한다.
     * 선택 횟수를 증가시키고 정규화된 점수를 재계산한다.
     */
    private void updateCategoryPreferences(Long memberId, List<RouteSelectionPlace> selectedPlaces) {
        if (selectedPlaces == null || selectedPlaces.isEmpty()) {
            return;
        }

        for (RouteSelectionPlace sp : selectedPlaces) {
            var category = sp.getPlace().getCategory();
            if (category == null) continue;

            UserCategoryPreference pref = preferenceRepository
                    .findByMemberIdAndCategory_Id(memberId, category.getId())
                    .orElseGet(() -> UserCategoryPreference.builder()
                            .memberId(memberId)
                            .category(category)
                            .selectionCount(0)
                            .preferenceScore(0.0)
                            .build());

            pref.incrementCount();
            preferenceRepository.save(pref);
        }

        // 정규화: 해당 사용자의 모든 카테고리 점수를 0~1로 재계산
        recalculatePreferenceScores(memberId);
    }

    private void recalculatePreferenceScores(Long memberId) {
        List<UserCategoryPreference> allPrefs = preferenceRepository.findByMemberId(memberId);
        int maxCount = allPrefs.stream()
                .mapToInt(UserCategoryPreference::getSelectionCount)
                .max()
                .orElse(1);

        for (UserCategoryPreference pref : allPrefs) {
            pref.updateScore((double) pref.getSelectionCount() / maxCount);
        }
        preferenceRepository.saveAll(allPrefs);
    }
}
