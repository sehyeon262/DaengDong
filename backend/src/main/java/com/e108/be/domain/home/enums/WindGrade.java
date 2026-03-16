package com.e108.be.domain.home.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 풍속 등급
 *
 * 기준:
 *   0~3 m/s   → CALM (고요)
 *   4~8 m/s   → LIGHT (약간)
 *   9~13 m/s  → STRONG (강함)
 *   14~ m/s   → VERY_STRONG (매우강함)
 */
@Getter
@RequiredArgsConstructor
public enum WindGrade {

    CALM("고요"),
    LIGHT("약간"),
    STRONG("강함"),
    VERY_STRONG("매우강함");

    private final String label;

    public static WindGrade from(double windSpeed) {
        if (windSpeed <= 3.0) return CALM;
        if (windSpeed <= 8.0) return LIGHT;
        if (windSpeed <= 13.0) return STRONG;
        return VERY_STRONG;
    }
}
