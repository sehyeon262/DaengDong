package com.e108.be.domain.diary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GmsAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.gms.api-key}")
    private String apiKey;

    @Value("${api.gms.model}")
    private String model;

    @Value("${api.gms.base-url}")
    private String baseUrl;

    public GmsAiClient(@Qualifier("gmsRestTemplate") RestTemplate restTemplate,
                        ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generate(String developerPrompt, String userPrompt) {
        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "developer", "content", developerPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("GMS AI API 호출 실패", e);
            throw new RuntimeException("일기 생성 AI 호출에 실패했습니다.", e);
        }
    }
}
