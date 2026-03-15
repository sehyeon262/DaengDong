package com.e108.be.domain.home.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 하늘 상태 코드
 *
 * 기상청 SKY 코드: 1(맑음), 3(구름많음), 4(흐림)
 * 기상청 PTY 코드: 0(없음), 1(비), 2(비/눈), 3(눈), 5(빗방울), 6(빗방울눈날림), 7(눈날림)
 */
@Getter
@RequiredArgsConstructor
public enum SkyStatus {

    SUNNY("맑음"),
    CLOUDY("흐림"),
    RAINY("비"),
    SNOWY("눈");

    private final String label;

    /**
     * 기상청 SKY + PTY 코드 → SkyStatus 변환
     * PTY(강수형태)가 우선: 비/눈이 있으면 하늘상태와 무관하게 RAINY/SNOWY
     */
    public static SkyStatus from(String skyCode, String ptyCode) {
        int pty = Integer.parseInt(ptyCode);

        // 비 계열 (비, 비/눈, 소나기, 빗방울, 빗방울눈날림)
        if (pty == 1 || pty == 2 || pty == 4 || pty == 5 || pty == 6) {
            return RAINY;
        }
        // 눈 계열 (눈, 눈날림)
        if (pty == 3 || pty == 7) {
            return SNOWY;
        }

        // 강수 없음 → SKY 코드로 판단
        int sky = Integer.parseInt(skyCode);
        if (sky == 1) {
            return SUNNY;
        }
        return CLOUDY; // 3(구름많음) or 4(흐림)
    }
}
