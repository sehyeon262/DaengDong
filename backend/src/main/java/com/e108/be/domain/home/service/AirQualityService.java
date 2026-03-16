package com.e108.be.domain.home.service;

import com.e108.be.domain.home.exception.AirQualityApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 에어코리아 미세먼지 API 연동 서비스
 *
 * - 위경도 → 가장 가까운 시도 → 시도별 실시간 측정정보 조회
 * - 30분 캐시 (미세먼지 데이터는 1시간 간격 갱신)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AirQualityService {

    private static final String AIR_QUALITY_API_URL =
            "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty";
    private static final long CACHE_TTL_MINUTES = 30;

    @Value("${api.air-quality.service-key}")
    private String serviceKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 시도 기준 캐시
    private final Map<String, CachedAirQuality> cache = new ConcurrentHashMap<>();

    public AirQualityData getAirQuality(double latitude, double longitude) {
        String sidoName = getSidoName(latitude, longitude);

        CachedAirQuality cached = cache.get(sidoName);
        if (cached != null && !cached.isExpired()) {
            log.debug("미세먼지 캐시 히트: sido={}", sidoName);
            return cached.data;
        }

        log.info("에어코리아 API 호출: sido={}", sidoName);
        AirQualityData data = fetchFromApi(sidoName);
        cache.put(sidoName, new CachedAirQuality(data, LocalDateTime.now()));
        return data;
    }

    private AirQualityData fetchFromApi(String sidoName) {
        try {
            String encodedSido = URLEncoder.encode(sidoName, StandardCharsets.UTF_8);
            String url = AIR_QUALITY_API_URL
                    + "?serviceKey=" + serviceKey
                    + "&returnType=json"
                    + "&numOfRows=1"
                    + "&pageNo=1"
                    + "&sidoName=" + encodedSido
                    + "&ver=1.0";

            // URI.create()로 이중 인코딩 방지 (서비스키에 %2B, %2F 등 포함)
            String response = restTemplate.getForObject(URI.create(url), String.class);
            return parseAirQualityResponse(response);

        } catch (AirQualityApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("에어코리아 API 호출 실패", e);
            throw new AirQualityApiException();
        }
    }

    private AirQualityData parseAirQualityResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("response").path("body").path("items");

            if (items.isMissingNode() || !items.isArray() || items.isEmpty()) {
                throw new AirQualityApiException("에어코리아 응답 형식이 올바르지 않습니다.");
            }

            JsonNode firstItem = items.get(0);
            String pm10Str = firstItem.path("pm10Value").asText("-1");
            String stationName = firstItem.path("stationName").asText("");

            int pm10Value = parsePm10Value(pm10Str);
            return new AirQualityData(pm10Value, stationName);

        } catch (AirQualityApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("에어코리아 응답 파싱 실패", e);
            throw new AirQualityApiException("미세먼지 응답을 처리할 수 없습니다.");
        }
    }

    /**
     * PM10 수치 파싱
     * 에어코리아 응답에서 "-" 또는 빈 값이 올 수 있음 → 기본값 0
     */
    private int parsePm10Value(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return Math.max(parsed, 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 위경도 → 가장 가까운 시도명 매핑
     * 각 시도 중심 좌표와의 거리 비교로 판정
     */
    private String getSidoName(double lat, double lng) {
        String closest = "서울";
        double minDist = Double.MAX_VALUE;

        for (Map.Entry<String, double[]> entry : SIDO_CENTERS.entrySet()) {
            double[] center = entry.getValue();
            double dist = Math.pow(lat - center[0], 2) + Math.pow(lng - center[1], 2);
            if (dist < minDist) {
                minDist = dist;
                closest = entry.getKey();
            }
        }
        return closest;
    }

    // 시도별 중심 좌표
    private static final Map<String, double[]> SIDO_CENTERS = Map.ofEntries(
            Map.entry("서울", new double[]{37.5665, 126.9780}),
            Map.entry("부산", new double[]{35.1796, 129.0756}),
            Map.entry("대구", new double[]{35.8714, 128.6014}),
            Map.entry("인천", new double[]{37.4563, 126.7052}),
            Map.entry("광주", new double[]{35.1595, 126.8526}),
            Map.entry("대전", new double[]{36.3504, 127.3845}),
            Map.entry("울산", new double[]{35.5384, 129.3114}),
            Map.entry("세종", new double[]{36.4800, 127.2890}),
            Map.entry("경기", new double[]{37.2750, 127.0095}),
            Map.entry("강원", new double[]{37.8228, 128.1555}),
            Map.entry("충북", new double[]{36.6357, 127.4912}),
            Map.entry("충남", new double[]{36.6588, 126.6728}),
            Map.entry("전북", new double[]{35.8203, 127.1088}),
            Map.entry("전남", new double[]{34.8161, 126.4629}),
            Map.entry("경북", new double[]{36.4919, 128.8889}),
            Map.entry("경남", new double[]{35.4606, 128.2132}),
            Map.entry("제주", new double[]{33.4996, 126.5312})
    );

    // --- 캐시 내부 클래스 ---

    private record CachedAirQuality(AirQualityData data, LocalDateTime cachedAt) {
        boolean isExpired() {
            return cachedAt.plusMinutes(CACHE_TTL_MINUTES).isBefore(LocalDateTime.now());
        }
    }

    /**
     * 에어코리아 API 응답 데이터
     */
    public record AirQualityData(int pm10Value, String stationName) {
    }
}
