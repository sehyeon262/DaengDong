package com.e108.be.domain.home.service;

import com.e108.be.domain.home.exception.WeatherApiException;
import com.e108.be.global.common.util.GridCoordinateConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 기상청 초단기예보 API 연동 서비스
 *
 * - 위경도를 격자좌표로 변환 후 기상청 API 호출
 * - 10분 캐시로 외부 API 호출 최소화
 * - 반환값: 기온, 풍속, 하늘상태, 강수형태, 습도
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private static final String WEATHER_API_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst";
    private static final long CACHE_TTL_MINUTES = 10;

    @Value("${api.weather.service-key}")
    private String serviceKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 격자좌표 기준 캐시
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    public WeatherData getWeather(double latitude, double longitude) {
        int[] grid = GridCoordinateConverter.convert(latitude, longitude);
        String cacheKey = grid[0] + "_" + grid[1];

        CachedWeather cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("날씨 캐시 히트: nx={}, ny={}", grid[0], grid[1]);
            return cached.data;
        }

        log.info("기상청 API 호출: nx={}, ny={}", grid[0], grid[1]);
        WeatherData data = fetchFromApi(grid[0], grid[1]);
        cache.put(cacheKey, new CachedWeather(data, LocalDateTime.now()));
        return data;
    }

    private WeatherData fetchFromApi(int nx, int ny) {
        try {
            // base_date, base_time 계산
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime baseTime = calculateBaseTime(now);
            String baseDate = baseTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String baseTimeStr = baseTime.format(DateTimeFormatter.ofPattern("HHmm"));

            String url = WEATHER_API_URL
                    + "?serviceKey=" + serviceKey
                    + "&numOfRows=60"
                    + "&pageNo=1"
                    + "&dataType=JSON"
                    + "&base_date=" + baseDate
                    + "&base_time=" + baseTimeStr
                    + "&nx=" + nx
                    + "&ny=" + ny;

            // URI.create()로 이중 인코딩 방지 (서비스키에 %2B, %2F 등 포함)
            String response = restTemplate.getForObject(URI.create(url), String.class);
            return parseWeatherResponse(response);

        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("기상청 API 호출 실패", e);
            throw new WeatherApiException();
        }
    }

    /**
     * 초단기예보 base_time 계산
     * - 매 시간 30분에 발표, 약 15분 후 조회 가능
     * - 안전하게: minute >= 45이면 현재시간:30, 아니면 (현재시간-1):30
     */
    private LocalDateTime calculateBaseTime(LocalDateTime now) {
        if (now.getMinute() >= 45) {
            return now.withMinute(30).withSecond(0).withNano(0);
        }
        return now.minusHours(1).withMinute(30).withSecond(0).withNano(0);
    }

    private WeatherData parseWeatherResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isMissingNode() || !items.isArray()) {
                throw new WeatherApiException("기상청 응답 형식이 올바르지 않습니다.");
            }

            int temperature = 0;
            double windSpeed = 0.0;
            int humidity = 0;
            String skyCode = "1";
            String ptyCode = "0";

            // 가장 가까운 예보 시간의 데이터를 추출
            for (JsonNode item : items) {
                String category = item.path("category").asText();
                String value = item.path("fcstValue").asText();

                switch (category) {
                    case "T1H" -> temperature = (int) Math.round(Double.parseDouble(value));
                    case "WSD" -> windSpeed = Double.parseDouble(value);
                    case "REH" -> humidity = Integer.parseInt(value);
                    case "SKY" -> skyCode = value;
                    case "PTY" -> ptyCode = value;
                }
            }

            int feelsLike = calculateFeelsLike(temperature, windSpeed, humidity);

            return new WeatherData(temperature, windSpeed, humidity, skyCode, ptyCode, feelsLike);

        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("기상청 응답 파싱 실패", e);
            throw new WeatherApiException("기상청 응답을 처리할 수 없습니다.");
        }
    }

    /**
     * 체감온도 계산
     * - 10°C 이하: 풍속 체감온도 공식 (Wind Chill)
     * - 그 외: 실제 기온 그대로 반환
     */
    private int calculateFeelsLike(int temp, double windSpeedMs, int humidity) {
        double windSpeedKmh = windSpeedMs * 3.6;

        if (temp <= 10 && windSpeedKmh >= 4.8) {
            double feelsLike = 13.12
                    + 0.6215 * temp
                    - 11.37 * Math.pow(windSpeedKmh, 0.16)
                    + 0.3965 * temp * Math.pow(windSpeedKmh, 0.16);
            return (int) Math.round(feelsLike);
        }

        return temp;
    }

    // --- 캐시 내부 클래스 ---

    private record CachedWeather(WeatherData data, LocalDateTime cachedAt) {
        boolean isExpired() {
            return cachedAt.plusMinutes(CACHE_TTL_MINUTES).isBefore(LocalDateTime.now());
        }
    }

    /**
     * 기상청 API 응답 데이터
     */
    public record WeatherData(
            int temperature,
            double windSpeed,
            int humidity,
            String skyCode,
            String ptyCode,
            int feelsLike
    ) {
    }
}
