package com.e108.be.domain.walk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * GPS 좌표 저장 및 거리 계산 서비스
 * - Redis에 GPS 좌표 버퍼링
 * - Haversine 공식으로 누적 거리 계산
 */
@Service
@RequiredArgsConstructor
public class WalkLocationService {

    // TODO: Redis 좌표 저장, 거리 계산 로직 구현
}
