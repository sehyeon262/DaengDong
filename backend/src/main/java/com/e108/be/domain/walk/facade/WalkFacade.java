package com.e108.be.domain.walk.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Walk Facade
 * 여러 서비스를 조합해서 하나의 유즈케이스를 처리
 * (선택적 사용)
 */
@Component
@RequiredArgsConstructor
public class WalkFacade {

    // TODO: WalkService, WalkLocationService, WalkSummaryService 조합
}
