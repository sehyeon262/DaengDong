package com.e108.be.domain.diary.service;

import com.e108.be.domain.diary.entity.Diary;
import com.e108.be.domain.diary.repository.DiaryRepository;
import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.home.service.WeatherService;
import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import com.e108.be.domain.place.repository.PlaceRepository;
import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiaryGenerationWorker {

    private static final String GPS_KEY_PREFIX = "walk:gps:";

    private final DiaryRepository diaryRepository;
    private final WalkRecordRepository walkRecordRepository;
    private final DogRepository dogRepository;
    private final PlaceRepository placeRepository;
    private final WeatherService weatherService;
    private final GmsAiClient gmsAiClient;
    private final DiaryPromptBuilder promptBuilder;
    private final RedisTemplate<String, String> redisTemplate;

    @Async("diaryExecutor")
    @Transactional
    public void generate(Long diaryId, Long walkId, Long dogId) {
        try {
            log.info("[일기생성] 시작: diaryId={}, walkId={}, dogId={}", diaryId, walkId, dogId);

            WalkRecord walk = walkRecordRepository.findById(walkId)
                    .orElseThrow(() -> new IllegalStateException("WalkRecord not found: " + walkId));
            Dog dog = dogRepository.findById(dogId)
                    .orElseThrow(() -> new IllegalStateException("Dog not found: " + dogId));

            // Redis에서 GPS 좌표로 날씨 + 근처 장소 조회
            WeatherService.WeatherData weather = fetchWeather(walkId);
            List<String> nearbyPlaceNames = fetchNearbyPlaces(walkId);
            log.debug("[일기생성] 데이터 수집 완료 - weather={}, places={}", weather != null, nearbyPlaceNames);

            // 프롬프트 구성 + LLM 호출
            String userPrompt = promptBuilder.buildUserPrompt(dog, walk, weather, nearbyPlaceNames);
            log.debug("[일기생성] 프롬프트 생성 완료, AI 호출 시작...");
            String content = gmsAiClient.generate(DiaryPromptBuilder.DEVELOPER_PROMPT, userPrompt);
            log.debug("[일기생성] AI 응답 수신: {}자", content != null ? content.length() : 0);

            // 일기 내용 저장
            Diary diary = diaryRepository.findById(diaryId).orElseThrow();
            diary.updateContent(content);
            diaryRepository.save(diary);

            log.info("[일기생성] 완료: diaryId={}, walkId={}", diaryId, walkId);

        } catch (Exception e) {
            log.error("[일기생성] 실패: walkId={}, diaryId={}, 에러={}", walkId, diaryId, e.getMessage(), e);
            // content가 null인 채로 남겨두면 프론트에서 로딩 표시 → 수동 재생성 가능
        }
    }

    private WeatherService.WeatherData fetchWeather(Long walkId) {
        try {
            String redisKey = GPS_KEY_PREFIX + walkId;
            List<String> points = redisTemplate.opsForList().range(redisKey, -1, -1);

            if (points != null && !points.isEmpty()) {
                String[] parts = points.get(0).split(",");
                double lat = Double.parseDouble(parts[0]);
                double lon = Double.parseDouble(parts[1]);
                return weatherService.getWeather(lat, lon);
            }
        } catch (Exception e) {
            log.warn("날씨 조회 실패, 날씨 없이 일기 생성", e);
        }
        return null;
    }

    /**
     * 산책 경로의 중간 지점 기준으로 반경 200m 내 장소 이름 최대 3개 조회
     */
    private List<String> fetchNearbyPlaces(Long walkId) {
        try {
            String redisKey = GPS_KEY_PREFIX + walkId;
            List<String> points = redisTemplate.opsForList().range(redisKey, 0, -1);

            if (points == null || points.isEmpty()) {
                return List.of();
            }

            // 산책 경로의 중간 지점 사용 (가장 대표적인 위치)
            String midPoint = points.get(points.size() / 2);
            String[] parts = midPoint.split(",");
            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);

            List<NearbyPlaceProjection> nearby = placeRepository.findNearby(lat, lon, 200, 3);
            List<String> names = new ArrayList<>();
            for (NearbyPlaceProjection place : nearby) {
                names.add(place.getName());
            }
            return names;

        } catch (Exception e) {
            log.warn("근처 장소 조회 실패, 장소 없이 일기 생성", e);
            return List.of();
        }
    }
}
