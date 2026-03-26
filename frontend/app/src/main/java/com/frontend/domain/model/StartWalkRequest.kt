package com.frontend.domain.model

/**
 * 산책 시작 요청 DTO (POST /walks)
 *
 * - 자유 산책: dogId만 필수, selectedType = null
 * - 추천 경로 산책: selectedType != null, 경로 정보 포함
 */
data class StartWalkRequest(
    val dogId: Long,

    // 경로 선택 정보 (자유 산책 시 null)
    val selectedType: String? = null,           // SHORT, RECOMMENDED, EXPLORE, WALK_ONLY
    val selectedDistanceM: Int? = null,         // 선택한 경로의 총 거리 (미터)
    val placeIds: List<Long>? = null,           // 경유지 ID 목록

    // 날씨 정보 (선택)
    val weatherCondition: String? = null,       // CLEAR, CLOUDY, RAIN, SNOW, HOT, COLD
    val temperature: Double? = null,            // 현재 기온 (°C)

    // 추천 경로 좌표 (이탈률 계산용)
    val recommendedPath: List<List<Double>>? = null  // [[lat, lon], [lat, lon], ...]
)
