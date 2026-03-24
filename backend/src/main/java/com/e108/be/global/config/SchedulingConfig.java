package com.e108.be.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 활성화 설정
 *
 * @Scheduled 어노테이션이 붙은 메서드가 주기적으로 실행되도록 한다.
 * 현재 활용처: ScoringWeightLearner (ML 가중치 학습 + 세그먼트 CF 갱신)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
