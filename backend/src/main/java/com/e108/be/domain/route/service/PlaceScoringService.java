package com.e108.be.domain.route.service;

import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import com.e108.be.domain.route.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 장소 스코어링
 *
 * 각 장소에 대해 거리, 카테고리 가중치, 방문이력 감쇠, 랜덤 노이즈를 종합하여
 * 단일 점수를 산출한다.
 *
 * score = WEIGHT_DISTANCE × (1 / (1 + 거리))
 *       + WEIGHT_CATEGORY × 카테고리 routeWeight 정규화
 *       × visitDecay(방문횟수)
 *       + noise (초기 사용자용)
 */
@Service
@RequiredArgsConstructor
public class PlaceScoringService {

    // --- 가중치 상수 ---
    private static final double WEIGHT_DISTANCE = 0.5;
    private static final double WEIGHT_CATEGORY = 0.5;

    // 카테고리 routeWeight 정규화 기준값 (place_category 테이블 최대값 기준)
    private static final double MAX_ROUTE_WEIGHT = 10.0;

    // 초기 사용자 랜덤 노이즈 범위
    private static final double NOISE_RANGE = 0.1;

    /**
     * 장소 하나에 대한 추천 점수 계산
     *
     * @param place     주변 장소 Projection
     * @param originLat 사용자 현재 위도
     * @param originLon 사용자 현재 경도
     * @return 0 이상의 점수 (높을수록 추천도 높음)
     */
    public double score(NearbyPlaceProjection place, double originLat, double originLon) {
        double score = 0.0;

        // 1. 거리 점수 (가까울수록 높음)
        double distanceM = place.getDistanceMeters() != null
                ? place.getDistanceMeters()
                : GeoUtils.haversine(originLat, originLon, place.getLatitude(), place.getLongitude());
        score += WEIGHT_DISTANCE * (1.0 / (1.0 + distanceM / 1000.0));

        // 2. 카테고리 가중치 점수
        int routeWeight = place.getRouteWeight() != null ? place.getRouteWeight() : 0;
        score += WEIGHT_CATEGORY * (routeWeight / MAX_ROUTE_WEIGHT);

        // 3. 랜덤 노이즈 (같은 출발지에서 매번 같은 결과 방지)
        // TODO: 방문이력 데이터 축적 후 visitDecay로 교체
        score += ThreadLocalRandom.current().nextDouble(-NOISE_RANGE, NOISE_RANGE);

        return Math.max(0, score);
    }

    // TODO: 방문이력 감쇠 - WalkPlace 테이블 구현 후 활성화
    // private double visitDecay(Long placeId, Long memberId) {
    //     int recentVisits = walkPlaceRepository.countRecentVisits(placeId, memberId, 7);
    //     return 1.0 / (1.0 + recentVisits);
    // }
}
