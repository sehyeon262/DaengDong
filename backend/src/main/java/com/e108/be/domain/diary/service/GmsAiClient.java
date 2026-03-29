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
                "max_tokens", 512,
                "messages", List.of(
                        Map.of("role", "developer", "content", developerPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            log.info("[GMS] 요청 시작 - url={}, model={}, promptLength={}", url, model, userPrompt.length());
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            long start = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            long elapsed = System.currentTimeMillis() - start;

            log.info("[GMS] 응답 수신 - status={}, elapsed={}ms, bodyLength={}",
                    response.getStatusCode(), elapsed,
                    response.getBody() != null ? response.getBody().length() : 0);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            log.info("[GMS] 파싱 완료 - contentLength={}", content.length());
            return content;

        } catch (Exception e) {
            log.error("[GMS] API 호출 실패 - url={}, model={}, error={}", url, model, e.getMessage(), e);
            throw new RuntimeException("일기 생성 AI 호출에 실패했습니다.", e);
        }
    }
}
