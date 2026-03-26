package com.e108.be.domain.route.entity;

/**
 * 날씨 상태 enum
 *
 * 경로 선택 로그에 기록되는 날씨 유형을 정의한다.
 * 자유 문자열 대신 enum으로 관리하여 데이터 정규화를 보장한다.
 */
public enum WeatherCondition {
    CLEAR,      // 맑음
    CLOUDY,     // 흐림
    RAIN,       // 비
    SNOW,       // 눈
    HOT,        // 더움 (30도 이상)
    COLD        // 추움 (0도 이하)
}
