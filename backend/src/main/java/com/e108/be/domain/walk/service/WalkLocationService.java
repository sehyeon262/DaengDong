package com.e108.be.domain.walk.service;

import com.e108.be.domain.walk.dto.request.WalkLocationRequest;
import com.e108.be.domain.walk.exception.WalkNotFoundException;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GPS 좌표 저장 서비스
 * - GPS 배치를 WKT(Well-Known Text)로 변환 후 route_line에 직접 저장
 * - PostgreSQL PostGIS ST_MakeLine으로 좌표를 이어붙임
 */
@Service
@RequiredArgsConstructor
public class WalkLocationService {

    private final WalkRecordRepository walkRecordRepository;

    /**
     * GPS 좌표 배치를 route_line에 저장
     * POST /walks/{walkId}/locations
     *
     * 좌표 목록을 WKT LINESTRING으로 변환 후 기존 route_line에 이어붙임
     * - route_line이 NULL이면 새 LINESTRING 생성
     * - 이미 있으면 끝에 새 좌표 추가
     *
     * WKT 예시: "LINESTRING(126.97 37.56, 126.98 37.57)"
     * 주의: WKT는 경도(X) 먼저, 위도(Y) 나중 순서
     */
    @Transactional
    public int saveLocations(Long walkId, WalkLocationRequest request) {
        walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        List<WalkLocationRequest.LocationPoint> locations = request.getLocations();
        if (locations == null || locations.isEmpty()) return 0;

        String wkt = buildWkt(locations);
        walkRecordRepository.appendToRouteLine(walkId, wkt);

        return locations.size();
    }

    /**
     * 좌표 목록 → WKT LINESTRING 변환
     * 좌표가 1개인 경우 같은 점을 2번 써서 유효한 LINESTRING 생성
     * (PostGIS LINESTRING은 최소 2개 좌표 필요)
     */
    private String buildWkt(List<WalkLocationRequest.LocationPoint> locations) {
        if (locations.size() == 1) {
            WalkLocationRequest.LocationPoint p = locations.get(0);
            return String.format("LINESTRING(%f %f, %f %f)",
                    p.getLongitude(), p.getLatitude(),
                    p.getLongitude(), p.getLatitude());
        }

        String coords = locations.stream()
                .map(p -> p.getLongitude() + " " + p.getLatitude())
                .collect(Collectors.joining(", "));

        return "LINESTRING(" + coords + ")";
    }
}
