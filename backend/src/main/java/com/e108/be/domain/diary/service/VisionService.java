package com.e108.be.domain.diary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 * Google Cloud Vision API (REST) - API 키 방식
 *
 * 사진에서 객체를 감지하여 강아지 여부 판단 + 주변 사물/동물 정보를 추출한다.
 */
@Service
@Slf4j
public class VisionService {

    private static final String VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate";

    private static final Set<String> DOG_LABELS = Set.of(
            "dog", "puppy", "canine"
    );

    private static final Set<String> ANIMAL_LABELS = Set.of(
            "dog", "puppy", "canine", "cat", "kitten", "feline",
            "bird", "duck", "pigeon", "squirrel", "rabbit", "hamster",
            "turtle", "fish", "butterfly", "insect"
    );

    private static final Set<String> SCENE_LABELS = Set.of(
            "park", "grass", "tree", "flower", "garden", "river", "lake",
            "beach", "mountain", "forest", "road", "sidewalk", "bridge",
            "playground", "bench", "fountain", "sky", "cloud", "sunset",
            "snow", "rain", "leaf", "trail", "path", "field"
    );

    private static final float MIN_CONFIDENCE = 0.7f;

    @Value("${google.vision.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public VisionService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * S3 이미지 URL을 분석하여 라벨 결과를 반환한다.
     */
    public VisionLabelResult analyze(String imageUrl) {
        try {
            // 이미지 다운로드 후 base64 인코딩
            byte[] imageBytes;
            try (InputStream is = URI.create(imageUrl).toURL().openStream()) {
                imageBytes = is.readAllBytes();
            }
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Vision API 요청 구성
            Map<String, Object> request = Map.of(
                    "requests", List.of(Map.of(
                            "image", Map.of("content", base64Image),
                            "features", List.of(Map.of(
                                    "type", "LABEL_DETECTION",
                                    "maxResults", 15
                            ))
                    ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = VISION_API_URL + "?key=" + apiKey;
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            // 에러 체크
            JsonNode responseNode = root.path("responses").get(0);
            if (responseNode.has("error")) {
                log.warn("[Vision] API 에러: {}", responseNode.path("error").path("message").asText());
                return null;
            }

            // 라벨 파싱
            List<String> allLabels = new ArrayList<>();
            List<String> animalLabels = new ArrayList<>();
            List<String> sceneLabels = new ArrayList<>();
            boolean hasDog = false;

            JsonNode annotations = responseNode.path("labelAnnotations");
            for (JsonNode annotation : annotations) {
                String label = annotation.path("description").asText();
                float score = (float) annotation.path("score").asDouble();

                if (score < MIN_CONFIDENCE) continue;

                allLabels.add(label);
                String lower = label.toLowerCase();

                if (DOG_LABELS.contains(lower)) {
                    hasDog = true;
                    animalLabels.add(label);
                } else if (ANIMAL_LABELS.contains(lower)) {
                    animalLabels.add(label);
                }

                if (SCENE_LABELS.contains(lower)) {
                    sceneLabels.add(label);
                }
            }

            log.info("[Vision] 분석 완료: labels={}, hasDog={}, animals={}, scenes={}",
                    allLabels, hasDog, animalLabels, sceneLabels);

            return new VisionLabelResult(allLabels, hasDog, animalLabels, sceneLabels);

        } catch (Exception e) {
            log.warn("[Vision] 분석 실패, 스킵: {}", imageUrl, e);
            return null;
        }
    }
}
