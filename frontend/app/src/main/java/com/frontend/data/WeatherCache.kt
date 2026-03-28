package com.frontend.data

import com.frontend.domain.model.HomeWeatherInfo

/**
 * 홈 화면에서 가져온 날씨 데이터를 산책 화면에서 재사용하기 위한 싱글턴 캐시.
 * - HomeViewModel이 /home API 응답 후 저장
 * - WalkViewModel이 경로 추천 시 먼저 확인 → 캐시 hit 시 /home API 재호출 생략
 */
object WeatherCache {
    @Volatile
    var weather: HomeWeatherInfo? = null
}
