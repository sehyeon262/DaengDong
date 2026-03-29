package com.e108.be.domain.route.service;

import com.e108.be.domain.route.client.TmapClient;
import com.e108.be.domain.route.client.TmapClient.TmapPathResult;
import com.e108.be.domain.route.client.TmapProperties;
import com.e108.be.domain.route.dto.response.LatLng;
import com.e108.be.domain.route.dto.response.RoutePlaceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TmapPathService {

    private final TmapClient tmapClient;
    private final TmapProperties tmapProperties;

    public PathResult getWalkingPath(LatLng origin, List<RoutePlaceResponse> places) {
        if (!tmapProperties.isEnabled()) {
            log.debug("TMAP disabled");
            return null;
        }

        if (places == null || places.isEmpty()) {
            return null;
        }

        List<LatLng> waypoints = places.stream()
                .map(p -> new LatLng(p.getLatitude(), p.getLongitude()))
                .toList();

        return toPathResult(tmapClient.getPath(origin, waypoints));
    }

    public PathResult getWalkingPath(
            LatLng origin,
            List<RoutePlaceResponse> passPlaces,
            LatLng destination
    ) {
        if (!tmapProperties.isEnabled()) {
            log.debug("TMAP disabled");
            return null;
        }

        List<LatLng> passPoints = passPlaces == null
                ? List.of()
                : passPlaces.stream()
                .map(p -> new LatLng(p.getLatitude(), p.getLongitude()))
                .toList();

        return toPathResult(tmapClient.getPath(origin, passPoints, destination));
    }

    public PathResult getDirectionalWalkingPath(LatLng origin, LatLng destination) {
        if (!tmapProperties.isEnabled()) {
            return null;
        }

        TmapPathResult outbound = tmapClient.getPathBetween(origin, destination);
        if (outbound == null) {
            return null;
        }

        TmapPathResult inbound = tmapClient.getPathBetween(destination, origin);
        if (inbound == null) {
            return new PathResult(
                    outbound.pathPoints(),
                    outbound.totalDistanceM() * 2,
                    outbound.estimatedMinutes() * 2,
                    "tmap",
                    true
            );
        }

        List<LatLng> combined = new ArrayList<>(outbound.pathPoints());
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

    private PathResult toPathResult(TmapPathResult result) {
        if (result == null) {
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

    public record PathResult(
            List<LatLng> pathPoints,
            int totalDistanceM,
            int estimatedMinutes,
            String provider,
            boolean roadBased
    ) {}
}
