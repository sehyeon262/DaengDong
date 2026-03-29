package com.e108.be.domain.route.client;

import com.e108.be.domain.route.dto.response.LatLng;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public TmapPathResult getPath(LatLng origin, List<LatLng> waypoints) {
        if (!properties.isEnabled()) {
            log.debug("TMAP API disabled");
            return null;
        }

        if (waypoints == null || waypoints.isEmpty()) {
            return getPathBetween(origin, origin);
        }

        return getPath(origin, waypoints, origin);
    }

    public TmapPathResult getPath(LatLng origin, List<LatLng> passPoints, LatLng destination) {
        if (!properties.isEnabled()) {
            log.debug("TMAP API disabled");
            return null;
        }

        if (destination == null) {
            log.warn("TMAP path skipped because destination is null");
            return null;
        }

        String passList = buildPassList(passPoints);
        return callTmapApi(origin, destination, passList);
    }

    public TmapPathResult getPathBetween(LatLng start, LatLng end) {
        return callTmapApi(start, end, null);
    }

    private TmapPathResult callTmapApi(LatLng start, LatLng end, String passList) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("appKey", properties.getAppKey());

            Map<String, Object> body = new HashMap<>();
            body.put("startX", String.valueOf(start.longitude()));
            body.put("startY", String.valueOf(start.latitude()));
            body.put("endX", String.valueOf(end.longitude()));
            body.put("endY", String.valueOf(end.latitude()));
            body.put("startName", "출발지");
            body.put("endName", "도착지");
            body.put("reqCoordType", "WGS84GEO");
            body.put("resCoordType", "WGS84GEO");
            body.put("searchOption", "0");

            if (passList != null && !passList.isEmpty()) {
                body.put("passList", passList);
            }

            String requestBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            log.debug("TMAP request: start=({}, {}), end=({}, {}), passList={}",
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
            log.warn("TMAP call failed: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("TMAP call failed", e);
            return null;
        }
    }

    private String buildPassList(List<LatLng> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < waypoints.size(); i++) {
            LatLng point = waypoints.get(i);
            sb.append(point.longitude()).append(",").append(point.latitude());
            if (i < waypoints.size() - 1) {
                sb.append("_");
            }
        }
        return sb.toString();
    }

    private TmapPathResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode features = root.path("features");

            if (features.isMissingNode() || !features.isArray()) {
                log.warn("TMAP response missing features");
                return null;
            }

            List<LatLng> pathPoints = new ArrayList<>();
            int totalDistanceM = 0;
            int totalTimeSeconds = 0;

            for (JsonNode feature : features) {
                JsonNode props = feature.path("properties");
                if (props.has("totalDistance")) {
                    totalDistanceM = props.path("totalDistance").asInt();
                }
                if (props.has("totalTime")) {
                    totalTimeSeconds = props.path("totalTime").asInt();
                }

                JsonNode geometry = feature.path("geometry");
                String type = geometry.path("type").asText();
                JsonNode coordinates = geometry.path("coordinates");

                if ("Point".equals(type)) {
                    double lon = coordinates.get(0).asDouble();
                    double lat = coordinates.get(1).asDouble();
                    addIfNotDuplicate(pathPoints, new LatLng(lat, lon));
                } else if ("LineString".equals(type)) {
                    for (JsonNode coord : coordinates) {
                        double lon = coord.get(0).asDouble();
                        double lat = coord.get(1).asDouble();
                        addIfNotDuplicate(pathPoints, new LatLng(lat, lon));
                    }
                }
            }

            if (pathPoints.isEmpty()) {
                log.warn("TMAP response contains no path points");
                return null;
            }

            int estimatedMinutes = (int) Math.ceil(totalTimeSeconds / 60.0);
            if (estimatedMinutes == 0 && totalDistanceM > 0) {
                estimatedMinutes = (int) Math.ceil(totalDistanceM / 67.0);
            }

            return new TmapPathResult(pathPoints, totalDistanceM, estimatedMinutes);
        } catch (Exception e) {
            log.error("TMAP response parsing failed", e);
            return null;
        }
    }

    private void addIfNotDuplicate(List<LatLng> list, LatLng point) {
        if (list.isEmpty()) {
            list.add(point);
            return;
        }

        LatLng last = list.get(list.size() - 1);
        if (Math.abs(last.latitude() - point.latitude()) > 0.000001
                || Math.abs(last.longitude() - point.longitude()) > 0.000001) {
            list.add(point);
        }
    }

    public record TmapPathResult(
            List<LatLng> pathPoints,
            int totalDistanceM,
            int estimatedMinutes
    ) {}
}
