package com.e108.be.domain.home.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 온도 등급
 *
 * 기준:
 *   ~4°C    → COLD (추움)
 *   5~14°C  → COOL (선선)
 *   15~22°C → COMFORTABLE (쾌적)
 *   23~32°C → HOT (더움)
 *   33°C~   → VERY_HOT (매우더움)
 */
@Getter
@RequiredArgsConstructor
public enum TemperatureGrade {

    COLD("추움"),
    COOL("선선"),
    COMFORTABLE("쾌적"),
    HOT("더움"),
    VERY_HOT("매우더움");

    private final String label;

    public static TemperatureGrade from(int temperature) {
        if (temperature <= 4) return COLD;
        if (temperature <= 14) return COOL;
        if (temperature <= 22) return COMFORTABLE;
        if (temperature <= 32) return HOT;
        return VERY_HOT;
    }
}
