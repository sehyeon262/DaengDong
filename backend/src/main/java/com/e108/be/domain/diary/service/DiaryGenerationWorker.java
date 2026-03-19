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
            WalkRecord walk = walkRecordRepository.findById(walkId).orElseThrow();
            Dog dog = dogRepository.findById(dogId).orElseThrow();

            // Redis에서 GPS 좌표로 날씨 + 근처 장소 조회
            WeatherService.WeatherData weather = fetchWeather(walkId);
            List<String> nearbyPlaceNames = fetchNearbyPlaces(walkId);

            // 프롬프트 구성 + LLM 호출
            String userPrompt = promptBuilder.buildUserPrompt(dog, walk, weather, nearbyPlaceNames);
            String content = gmsAiClient.generate(DiaryPromptBuilder.DEVELOPER_PROMPT, userPrompt);

            // 일기 내용 저장
            Diary diary = diaryRepository.findById(diaryId).orElseThrow();
            diary.updateContent(content);
            diaryRepository.save(diary);

            log.info("일기 생성 완료: diaryId={}, walkId={}", diaryId, walkId);

        } catch (Exception e) {
            log.error("일기 생성 실패: walkId={}, diaryId={}", walkId, diaryId, e);
            // 실패한 Diary row 삭제 → 재생성 가능하도록
            diaryRepository.deleteById(diaryId);
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
