package com.e108.be.domain.route.service;

import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import com.e108.be.domain.place.repository.PlaceRepository;
import com.e108.be.domain.route.dto.response.RouteDetailResponse;
import com.e108.be.domain.route.dto.response.RouteRecommendResponse;
import com.e108.be.domain.route.exception.RouteGenerationException;
import com.e108.be.domain.route.service.RouteGeneratorService.ScoredPlace;
import com.e108.be.domain.route.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 경로 추천 오케스트레이션 서비스
 *
 * 전체 흐름:
 * 1. 반경 내 후보 장소 조회 (PostGIS)
 * 2. 각 장소 스코어링
 * 3. 3개 경로 생성 (최단, 추천, 탐험)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private static final int DEFAULT_TARGET_MINUTES = 30;
    private static final int CANDIDATE_LIMIT = 30; // 후보 장소 최대 개수
    private static final int MIN_PLACES_REQUIRED = 3; // 경로 생성 최소 장소 수

    private final PlaceRepository placeRepository;
    private final PlaceScoringService placeScoringService;
    private final RouteGeneratorService routeGeneratorService;

    /**
     * 경로 추천
     *
     * @param lat           기준 위도
     * @param lon           기준 경도
     * @param targetMinutes 목표 산책 시간 (분), null이면 기본값 30
     * @param dogId         반려견 ID
     * @return 3개 경로가 담긴 응답
     */
    public RouteRecommendResponse recommend(double lat, double lon, Integer targetMinutes, Long dogId) {

        int minutes = targetMinutes != null ? targetMinutes : DEFAULT_TARGET_MINUTES;

        // 1단계: 검색 반경 산출 → 후보 장소 조회
        double radiusM = GeoUtils.estimateRadius(minutes);
        List<NearbyPlaceProjection> candidates = placeRepository.findNearby(lat, lon, radiusM, CANDIDATE_LIMIT);

        if (candidates.size() < MIN_PLACES_REQUIRED) {
            throw new RouteGenerationException();
        }

        // 2단계: 스코어링
        List<ScoredPlace> scoredPlaces = candidates.stream()
                .map(place -> new ScoredPlace(
                        place,
                        placeScoringService.score(place, lat, lon)
                ))
                .sorted(Comparator.comparingDouble(ScoredPlace::score).reversed())
                .toList();

        // 3단계: 3개 경로 생성
        List<RouteDetailResponse> routes = routeGeneratorService.generateRoutes(lat, lon, scoredPlaces);

        return RouteRecommendResponse.from(routes);
    }
}
