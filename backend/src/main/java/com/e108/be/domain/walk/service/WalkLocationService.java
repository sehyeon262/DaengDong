package com.e108.be.domain.walk.service;

import com.e108.be.domain.walk.dto.request.WalkLocationRequest;
import com.e108.be.domain.walk.exception.WalkNotFoundException;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * GPS 좌표 저장 및 거리 계산 서비스
 * - Redis에 GPS 좌표 버퍼링
 * - Haversine 공식으로 누적 거리 계산
 */
@Service
@RequiredArgsConstructor
public class WalkLocationService {

    private static final String GPS_KEY_PREFIX = "walk:gps:";

    private final WalkRecordRepository walkRecordRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * GPS 좌표 배치 저장
     * POST /walks/{walkId}/locations
     *
     * FE에서 보낸 좌표 목록을 Redis List에 순서대로 저장
     * Redis 키: "walk:gps:{walkId}"
     * 저장 형식: "위도,경도,타임스탬프"
     */
    public int saveLocations(Long walkId, WalkLocationRequest request) {
        walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        String redisKey = GPS_KEY_PREFIX + walkId;

        for (WalkLocationRequest.LocationPoint point : request.getLocations()) {
            String value = point.getLatitude() + "," + point.getLongitude() + "," + point.getTimestamp();
            redisTemplate.opsForList().rightPush(redisKey, value);
        }

        return request.getLocations().size();
    }
}
