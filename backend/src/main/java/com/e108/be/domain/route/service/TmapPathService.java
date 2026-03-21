package com.e108.be.domain.route.service;

import com.e108.be.domain.route.client.TmapClient;
import com.e108.be.domain.route.client.TmapClient.TmapPathResult;
import com.e108.be.domain.route.client.TmapProperties;
import com.e108.be.domain.route.dto.response.LatLng;
import com.e108.be.domain.route.dto.response.RoutePlaceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 도보 경로 서비스
 *
 * TMAP API를 통해 실제 도로 기반 경로를 조회한다.
 * TMAP 실패 시 기존 직선 경로를 fallback으로 사용한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TmapPathService {

    private final TmapClient tmapClient;
    private final TmapProperties tmapProperties;

    /**
     * 경유지 기반 도보 경로 조회
     *
     * @param origin 출발지 좌표
     * @param places 경유지 목록 (순서대로)
     * @return TMAP 경로 결과, 실패 시 null
     */
    public PathResult getWalkingPath(LatLng origin, List<RoutePlaceResponse> places) {
        if (!tmapProperties.isEnabled()) {
            log.debug("TMAP 비활성화 - null 반환");
            return null;
        }

        if (places == null || places.isEmpty()) {
            log.debug("경유지 없음 - 순수 산책 경로 조회");
            return null;
        }

        // 경유지를 LatLng 목록으로 변환
        List<LatLng> waypoints = places.stream()
                .map(p -> new LatLng(p.getLatitude(), p.getLongitude()))
                .toList();

        TmapPathResult result = tmapClient.getPath(origin, waypoints);

        if (result == null) {
            log.warn("TMAP 경로 조회 실패 - fallback 사용 예정");
            return null;
        }

        return new PathResult(
                result.pathPoints(),
                result.totalDistanceM(),
                result.estimatedMinutes(),
                "tmap",
                true
        );
    }

    /**
     * 방향 기반 순수 산책 경로 조회
     *
     * @param origin      출발지
     * @param destination 목적지 (왕복 시 다시 origin으로)
     * @return TMAP 경로 결과, 실패 시 null
     */
    public PathResult getDirectionalWalkingPath(LatLng origin, LatLng destination) {
        if (!tmapProperties.isEnabled()) {
            return null;
        }

        // 왕복 경로: origin -> destination -> origin
        TmapPathResult outbound = tmapClient.getPathBetween(origin, destination);
        if (outbound == null) {
            return null;
        }

        TmapPathResult inbound = tmapClient.getPathBetween(destination, origin);
        if (inbound == null) {
            // 편도라도 사용
            return new PathResult(
                    outbound.pathPoints(),
                    outbound.totalDistanceM() * 2, // 왕복 추정
                    outbound.estimatedMinutes() * 2,
                    "tmap",
                    true
            );
        }

        // 왕복 경로 병합
        List<LatLng> combined = new java.util.ArrayList<>(outbound.pathPoints());
        // 복귀 경로에서 첫 번째 점(destination)은 중복이므로 제외
        if (!inbound.pathPoints().isEmpty()) {
            combined.addAll(inbound.pathPoints().subList(1, inbound.pathPoints().size()));
        }

        return new PathResult(
                combined,
                outbound.totalDistanceM() + inbound.totalDistanceM(),
                outbound.estimatedMinutes() + inbound.estimatedMinutes(),
                "tmap",
                true
        );
    }

    /**
     * TMAP 경로 결과
     */
    public record PathResult(
            List<LatLng> pathPoints,
            int totalDistanceM,
            int estimatedMinutes,
            String provider,
            boolean roadBased
    ) {}
}
