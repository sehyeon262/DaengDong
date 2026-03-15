package com.e108.be.domain.home.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 미세먼지(PM10) 등급 — 환경부 기준
 *
 * 기준:
 *   0~15    → BEST (최상)
 *   16~30   → GOOD (좋음)
 *   31~80   → NORMAL (보통)
 *   81~150  → BAD (나쁨)
 *   151~    → VERY_BAD (매우나쁨)
 */
@Getter
@RequiredArgsConstructor
public enum FineDustGrade {

    BEST("최상"),
    GOOD("좋음"),
    NORMAL("보통"),
    BAD("나쁨"),
    VERY_BAD("매우나쁨");

    private final String label;

    public static FineDustGrade from(int pm10Value) {
        if (pm10Value <= 15) return BEST;
        if (pm10Value <= 30) return GOOD;
        if (pm10Value <= 80) return NORMAL;
        if (pm10Value <= 150) return BAD;
        return VERY_BAD;
    }
}
