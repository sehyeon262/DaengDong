package com.e108.be.domain.route.service;

import com.e108.be.domain.dog.entity.Dog;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @CacheEvict(value = "walkPattern", key = "#memberId")
    @Transactional
    public void logSelection(Long memberId, RouteSelectionRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Long dogId = dogRepository.findFirstByUser_Id(memberId)
                .map(Dog::getId)
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

        // 선택된 장소 연결 (요청 순서 보장)
        if (request.getPlaceIds() != null && !request.getPlaceIds().isEmpty()) {
            List<Long> placeIds = request.getPlaceIds();
            List<Place> places = placeRepository.findAllByIdWithCategory(placeIds);

            Map<Long, Place> placeMap = places.stream()
                    .collect(Collectors.toMap(Place::getId, Function.identity()));

            for (int i = 0; i < placeIds.size(); i++) {
                Place place = placeMap.get(placeIds.get(i));
                if (place == null) continue;

                RouteSelectionPlace selectionPlace = RouteSelectionPlace.builder()
                        .place(place)
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
     * 기존 선호도를 한 번에 조회 후, 일괄 갱신하여 쿼리 수를 최소화한다.
     */
    private void updateCategoryPreferences(Long memberId, List<RouteSelectionPlace> selectedPlaces) {
        if (selectedPlaces == null || selectedPlaces.isEmpty()) {
            return;
        }

        // 기존 선호도 한 번에 조회
        Map<Integer, UserCategoryPreference> existingPrefs = preferenceRepository
                .findByMemberId(memberId).stream()
                .collect(Collectors.toMap(
                        p -> p.getCategory().getId(),
                        Function.identity()));

        for (RouteSelectionPlace sp : selectedPlaces) {
            var category = sp.getPlace().getCategory();
            if (category == null) continue;

            UserCategoryPreference pref = existingPrefs.computeIfAbsent(
                    category.getId(),
                    k -> UserCategoryPreference.builder()
                            .memberId(memberId)
                            .category(category)
                            .selectionCount(0)
                            .preferenceScore(0.0)
                            .build());

            pref.incrementCount();
        }

        // 정규화: 0~1 점수 재계산 후 일괄 저장
        int maxCount = existingPrefs.values().stream()
                .mapToInt(UserCategoryPreference::getSelectionCount)
                .max()
                .orElse(1);

        for (UserCategoryPreference pref : existingPrefs.values()) {
            pref.updateScore((double) pref.getSelectionCount() / maxCount);
        }

        preferenceRepository.saveAll(existingPrefs.values());
    }
}
