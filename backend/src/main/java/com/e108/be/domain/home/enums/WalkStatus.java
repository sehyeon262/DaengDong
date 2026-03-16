package com.e108.be.domain.home.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 산책 적합도
 *
 * 판정 로직:
 *   극단값 하나라도 해당 → BAD
 *   나쁜 조건 하나 이상 → CAUTION
 *   보통 조건            → GOOD
 *   전부 좋은 조건       → GREAT
 */
@Getter
@RequiredArgsConstructor
public enum WalkStatus {

    GREAT("최상"),
    GOOD("좋음"),
    CAUTION("주의"),
    BAD("비추천");

    private final String label;

    /**
     * 날씨/온도/미세먼지/풍속 종합 판정
     */
    public static WalkStatus calculate(TemperatureGrade tempGrade,
                                       FineDustGrade dustGrade,
                                       WindGrade windGrade,
                                       SkyStatus skyStatus) {
        // 극단값 → 무조건 BAD
        if (tempGrade == TemperatureGrade.VERY_HOT) return BAD;
        if (dustGrade == FineDustGrade.VERY_BAD) return BAD;
        if (windGrade == WindGrade.VERY_STRONG) return BAD;

        // 나쁜 조건 → CAUTION
        if (tempGrade == TemperatureGrade.COLD || tempGrade == TemperatureGrade.HOT) return CAUTION;
        if (dustGrade == FineDustGrade.BAD) return CAUTION;
        if (windGrade == WindGrade.STRONG) return CAUTION;
        if (skyStatus == SkyStatus.RAINY || skyStatus == SkyStatus.SNOWY) return CAUTION;

        // 보통 조건이 하나라도 있으면 → GOOD
        if (dustGrade == FineDustGrade.NORMAL
                || windGrade == WindGrade.LIGHT
                || skyStatus == SkyStatus.CLOUDY) {
            return GOOD;
        }

        // 전부 좋은 조건 → GREAT
        return GREAT;
    }
}
