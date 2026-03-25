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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final DogEmotionAnalyzer emotionAnalyzer;
    private final VisionService visionService;

    @Async("diaryExecutor")
    @Transactional
    public void generate(Long diaryId, Long walkId, Long dogId) {
        try {
            log.info("[일기생성] 시작: diaryId={}, walkId={}, dogId={}", diaryId, walkId, dogId);

            WalkRecord walk = walkRecordRepository.findById(walkId)
                    .orElseThrow(() -> new IllegalStateException("WalkRecord not found: " + walkId));
            Dog dog = dogRepository.findById(dogId)
                    .orElseThrow(() -> new IllegalStateException("Dog not found: " + dogId));

            // 날씨 + 장소를 비동기로 조회
            CompletableFuture<WeatherService.WeatherData> weatherFuture =
                    CompletableFuture.supplyAsync(() -> fetchWeather(walkId));
            CompletableFuture<List<String>> placesFuture =
                    CompletableFuture.supplyAsync(() -> fetchNearbyPlaces(walkId));

            // 1단계: Vision API로 사진 라벨 분석 (강아지 탐지 + 주변 사물)
            Map<String, VisionLabelResult> visionResults = analyzeAllPhotosWithVision(walk.getPhotoUrls());

            // 2단계: 강아지가 감지된 사진만 감정 분석 실행
            List<String> dogPhotoUrls = visionResults.entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue().hasDog())
                    .map(Map.Entry::getKey)
                    .toList();
            Map<String, EmotionResult> photoResults = analyzeAllPhotos(dogPhotoUrls);

            WeatherService.WeatherData weather = weatherFuture.join();
            List<String> nearbyPlaceNames = placesFuture.join();
            log.debug("[일기생성] 데이터 수집 완료 - weather={}, places={}, vision={}장, emotion={}장",
                    weather != null, nearbyPlaceNames, visionResults.size(), photoResults.size());

            EmotionResult bestResult = findBestResult(photoResults);

            // 프롬프트 구성 + LLM 호출 (Vision 라벨 + 감정 분석 결과 모두 반영)
            String userPrompt = promptBuilder.buildUserPrompt(dog, walk, weather, nearbyPlaceNames, photoResults, visionResults);
            log.debug("[일기생성] 프롬프트 생성 완료, AI 호출 시작...");
            String content = gmsAiClient.generate(DiaryPromptBuilder.DEVELOPER_PROMPT, userPrompt);
            log.debug("[일기생성] AI 응답 수신: {}자", content != null ? content.length() : 0);

            // 일기 내용 + 대표 감정 태그 + 사진별 감정 저장
            Diary diary = diaryRepository.findById(diaryId).orElseThrow();
            diary.updateContent(content);
            if (bestResult != null) {
                diary.updateEmotionTag(bestResult.emotionTag());
            }
            if (!photoResults.isEmpty()) {
                Map<String, String> photoEmotions = new LinkedHashMap<>();
                photoResults.forEach((url, result) -> photoEmotions.put(url, result.emotionTag()));
                diary.updatePhotoEmotions(photoEmotions);
            }
            diaryRepository.save(diary);

            log.info("[일기생성] 완료: diaryId={}, walkId={}, emotionTag={}, 사진분석={}장",
                    diaryId, walkId,
                    bestResult != null ? bestResult.emotionTag() : "없음",
                    photoResults.size());

        } catch (Exception e) {
            log.error("[일기생성] 실패: walkId={}, diaryId={}, 에러={}", walkId, diaryId, e.getMessage(), e);
            // 실패 시 기본 일기 내용 저장 (프론트가 무한 폴링하지 않도록)
            try {
                Diary diary = diaryRepository.findById(diaryId).orElse(null);
                if (diary != null && diary.getContent() == null) {
                    diary.updateContent("오늘도 즐거운 산책이었어요! 다음에 또 가자~ 🐾");
                    diaryRepository.save(diary);
                    log.info("[일기생성] 기본 일기로 대체 저장: diaryId={}", diaryId);
                }
            } catch (Exception fallbackErr) {
                log.error("[일기생성] 기본 일기 저장도 실패: diaryId={}", diaryId, fallbackErr);
            }
        }
    }

    /**
     * Vision API로 모든 사진의 라벨을 분석한다 (강아지 탐지 + 주변 사물/동물).
     */
    private Map<String, VisionLabelResult> analyzeAllPhotosWithVision(List<String> photoUrls) {
        Map<String, VisionLabelResult> results = new LinkedHashMap<>();

        if (photoUrls == null || photoUrls.isEmpty()) {
            return results;
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(photoUrls.size(), 4));
        Map<String, CompletableFuture<VisionLabelResult>> futures = new LinkedHashMap<>();

        for (String url : photoUrls) {
            futures.put(url, CompletableFuture.supplyAsync(() -> visionService.analyze(url), executor));
        }

        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        for (String url : photoUrls) {
            VisionLabelResult result = futures.get(url).join();
            if (result != null) {
                results.put(url, result);
            }
        }

        long dogCount = results.values().stream().filter(VisionLabelResult::hasDog).count();
        log.info("[일기생성] Vision 분석 완료: 전체 {}장, 강아지 감지 {}장", results.size(), dogCount);
        return results;
    }

    /**
     * 강아지가 감지된 사진에 대해 감정 분석을 수행한다.
     * 이미지 다운로드+전처리는 병렬, ONNX 추론은 순차(세션이 thread-safe하지 않음).
     *
     * @return URL → EmotionResult 맵 (강아지가 감지된 사진만 포함)
     */
    private Map<String, EmotionResult> analyzeAllPhotos(List<String> photoUrls) {
        Map<String, EmotionResult> results = new LinkedHashMap<>();

        if (photoUrls == null || photoUrls.isEmpty() || !emotionAnalyzer.isAvailable()) {
            return results;
        }

        // 1단계: 이미지 다운로드 + 전처리를 병렬로 실행 (I/O 바운드)
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(photoUrls.size(), 4));
        Map<String, CompletableFuture<float[][][][]>> prepFutures = new LinkedHashMap<>();

        for (String url : photoUrls) {
            prepFutures.put(url, CompletableFuture.supplyAsync(() -> {
                try {
                    return emotionAnalyzer.prepareImage(url);
                } catch (Exception e) {
                    log.warn("[일기생성] 이미지 전처리 실패, 스킵: {}", url, e);
                    return null;
                }
            }, executor));
        }

        // 모든 다운로드 완료 대기
        CompletableFuture.allOf(prepFutures.values().toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // 2단계: ONNX 추론은 순차 실행 (세션이 thread-safe하지 않음)
        for (String url : photoUrls) {
            try {
                float[][][][] inputData = prepFutures.get(url).join();
                if (inputData == null) continue;

                EmotionResult result = emotionAnalyzer.infer(url, inputData);
                if (result != null) {
                    results.put(url, result);
                    log.debug("[일기생성] 사진 감정: {} → {} (신뢰도: {})", url, result.emotionTag(), result.confidence());
                }
            } catch (Exception e) {
                log.warn("[일기생성] 사진 감정 분석 실패, 스킵: {}", url, e);
            }
        }

        log.info("[일기생성] 사진 감정 분석 완료: 전체 {}장 중 {}장 분석됨", photoUrls.size(), results.size());
        return results;
    }

    /**
     * 분석 결과 중 가장 높은 신뢰도의 결과를 대표 감정으로 선택한다.
     */
    private EmotionResult findBestResult(Map<String, EmotionResult> photoResults) {
        return photoResults.values().stream()
                .max((a, b) -> Float.compare(a.confidence(), b.confidence()))
                .orElse(null);
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
