package com.e108.be.domain.route.client;

import com.e108.be.domain.route.dto.response.LatLng;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TMAP 도보 길찾기 API 클라이언트
 *
 * TMAP raw 좌표(x=경도, y=위도)를 백엔드 공용 DTO(latitude, longitude)로 정규화하여 반환한다.
 */
@Component
@Slf4j
public class TmapClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TmapProperties properties;

    public TmapClient(@Qualifier("tmapRestTemplate") RestTemplate restTemplate,
                      ObjectMapper objectMapper,
                      TmapProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 도보 경로 조회 (경유지 포함)
     *
     * @param origin    출발지 좌표
     * @param waypoints 경유지 목록 (순서대로)
     * @return 실제 도로 기반 경로 좌표 목록, 실패 시 null
     */
    public TmapPathResult getPath(LatLng origin, List<LatLng> waypoints) {
        if (!properties.isEnabled()) {
            log.debug("TMAP API 비활성화 상태");
            return null;
        }

        if (waypoints == null || waypoints.isEmpty()) {
            // 경유지 없으면 origin만 왕복
            return getPathBetween(origin, origin);
        }

        // 경유지가 있는 경우: origin -> waypoints -> origin
        LatLng destination = origin; // 원점으로 복귀
        String passListStr = buildPassList(waypoints);

        return callTmapApi(origin, destination, passListStr);
    }

    /**
     * 두 지점 간 도보 경로 조회
     */
    public TmapPathResult getPathBetween(LatLng start, LatLng end) {
        return callTmapApi(start, end, null);
    }

    /**
     * TMAP API 호출
     */
    private TmapPathResult callTmapApi(LatLng start, LatLng end, String passList) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("appKey", properties.getAppKey());

            // TMAP은 x=경도(longitude), y=위도(latitude) 순서
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("startX", String.valueOf(start.longitude()));
            body.put("startY", String.valueOf(start.latitude()));
            body.put("endX", String.valueOf(end.longitude()));
            body.put("endY", String.valueOf(end.latitude()));
            body.put("startName", "출발지");
            body.put("endName", "도착지");
            body.put("reqCoordType", "WGS84GEO");
            body.put("resCoordType", "WGS84GEO");
            body.put("searchOption", "0"); // 추천 경로

            if (passList != null && !passList.isEmpty()) {
                body.put("passList", passList);
            }

            String requestBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            log.debug("TMAP API 요청: start=({}, {}), end=({}, {}), passList={}",
                    start.latitude(), start.longitude(),
                    end.latitude(), end.longitude(),
                    passList);

            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getBaseUrl() + "?version=1",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return parseResponse(response.getBody());

        } catch (RestClientException e) {
            log.warn("TMAP API 호출 실패 (네트워크): {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("TMAP API 호출 중 예외 발생", e);
            return null;
        }
    }

    /**
     * 경유지 목록을 TMAP passList 형식으로 변환
     * 형식: "lon1,lat1_lon2,lat2_..."
     */
    private String buildPassList(List<LatLng> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < waypoints.size(); i++) {
            LatLng point = waypoints.get(i);
            // TMAP은 경도,위도 순서
            sb.append(point.longitude()).append(",").append(point.latitude());
            if (i < waypoints.size() - 1) {
                sb.append("_");
            }
        }
        return sb.toString();
    }

    /**
     * TMAP 응답 파싱
     *
     * TMAP은 coordinates를 [경도, 위도] 순서로 반환하므로
     * 백엔드 공용 DTO인 LatLng(latitude, longitude)로 변환한다.
     */
    private TmapPathResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode features = root.path("features");

            if (features.isMissingNode() || !features.isArray()) {
                log.warn("TMAP 응답에 features 없음");
                return null;
            }

            List<LatLng> pathPoints = new ArrayList<>();
            int totalDistanceM = 0;
            int totalTimeSeconds = 0;

            for (JsonNode feature : features) {
                // properties에서 거리/시간 추출 (첫 번째 feature의 totalDistance/totalTime 사용)
                JsonNode props = feature.path("properties");
                if (props.has("totalDistance")) {
                    totalDistanceM = props.path("totalDistance").asInt();
                }
                if (props.has("totalTime")) {
                    totalTimeSeconds = props.path("totalTime").asInt();
                }

                // geometry 처리
                JsonNode geometry = feature.path("geometry");
                String type = geometry.path("type").asText();
                JsonNode coordinates = geometry.path("coordinates");

                if ("Point".equals(type)) {
                    // [경도, 위도] -> LatLng(위도, 경도)
                    double lon = coordinates.get(0).asDouble();
                    double lat = coordinates.get(1).asDouble();
                    addIfNotDuplicate(pathPoints, new LatLng(lat, lon));

                } else if ("LineString".equals(type)) {
                    // [[경도, 위도], [경도, 위도], ...]
                    for (JsonNode coord : coordinates) {
                        double lon = coord.get(0).asDouble();
                        double lat = coord.get(1).asDouble();
                        addIfNotDuplicate(pathPoints, new LatLng(lat, lon));
                    }
                }
            }

            if (pathPoints.isEmpty()) {
                log.warn("TMAP 응답에서 경로 좌표 추출 실패");
                return null;
            }

            int estimatedMinutes = (int) Math.ceil(totalTimeSeconds / 60.0);
            if (estimatedMinutes == 0 && totalDistanceM > 0) {
                // 시간 정보 없으면 4km/h 기준으로 계산
                estimatedMinutes = (int) Math.ceil(totalDistanceM / 67.0);
            }

            log.debug("TMAP 경로 파싱 완료: {} 포인트, {}m, {}분",
                    pathPoints.size(), totalDistanceM, estimatedMinutes);

            return new TmapPathResult(pathPoints, totalDistanceM, estimatedMinutes);

        } catch (Exception e) {
            log.error("TMAP 응답 파싱 실패", e);
            return null;
        }
    }

    /**
     * 중복 좌표 방지
     */
    private void addIfNotDuplicate(List<LatLng> list, LatLng point) {
        if (list.isEmpty()) {
            list.add(point);
            return;
        }
        LatLng last = list.get(list.size() - 1);
        // 소수점 6자리 수준에서 동일하면 중복으로 간주
        if (Math.abs(last.latitude() - point.latitude()) > 0.000001 ||
            Math.abs(last.longitude() - point.longitude()) > 0.000001) {
            list.add(point);
        }
    }

    /**
     * TMAP 경로 결과
     */
    public record TmapPathResult(
            List<LatLng> pathPoints,
            int totalDistanceM,
            int estimatedMinutes
    ) {}
}
