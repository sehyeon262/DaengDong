package com.e108.be.domain.maps.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * M3-01 GET /api/v1/maps/footprints 응답
 * 강아지의 모든 완료된 산책 경로를 히트맵용으로 반환
 *
 * routes: 산책별 좌표 목록 [ [[lat, lng], ...], [...] ]
 */
@Getter
@RequiredArgsConstructor
public class FootprintMapResponse {

    private final List<List<double[]>> routes;
}
