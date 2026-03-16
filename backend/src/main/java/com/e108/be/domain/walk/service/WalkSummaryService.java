package com.e108.be.domain.walk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 산책 요약 정보 서비스
 * - 거리 조회: GET /walks/{walkId}/distance
 * - 칼로리 조회: GET /walks/{walkId}/calories
 */
@Service
@RequiredArgsConstructor
public class WalkSummaryService {

    // TODO: 거리 조회, 칼로리 계산 로직 구현
}
