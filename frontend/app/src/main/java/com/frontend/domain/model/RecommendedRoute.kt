package com.frontend.domain.model

/**
 * 경로 추천 API 응답 (/api/v1/routes/recommend)
 */
data class RouteRecommendResponse(
    val fallbackLevel: String,          // NORMAL, REDUCED, WALK_ONLY
    val fallbackMessage: String?,       // 폴백 시 안내 메시지
    val routes: List<RecommendedRoute>
)

/**
 * 개별 추천 경로
 */
data class RecommendedRoute(
    val name: String,                   // 경로 이름 (빠른 산책, 추천 코스, 탐험 코스)
    val type: String,                   // SHORT, RECOMMENDED, EXPLORE, WALK_ONLY
    val totalDistanceM: Int,            // 총 거리 (미터)
    val estimatedMinutes: Int,          // 예상 소요 시간 (분)
    val places: List<RoutePlaceInfo>,   // 경유지 목록
    val polyline: List<LatLngPoint>,    // 직선 경로 좌표 (preview용)
    val actualPathPoints: List<LatLngPoint>?, // 실제 도로 기반 경로 좌표 (TMAP)
    val provider: String?,              // tmap 또는 preview
    val roadBased: Boolean?             // 실제 도로 기반 여부
) {
    /**
     * 지도에 그릴 경로 포인트 반환
     * actualPathPoints가 있으면 사용, 없으면 polyline 사용
     */
    fun getPathPoints(): List<LatLngPoint> = actualPathPoints ?: polyline

    /**
     * 거리를 km 단위로 반환
     */
    fun distanceKm(): Float = totalDistanceM / 1000f

    /**
     * subtitle 생성 (type에 따라)
     */
    fun getSubtitle(): String = when (type) {
        "SHORT" -> "가까운 코스"
        "RECOMMENDED" -> "추천 코스"
        "EXPLORE" -> "탐험 코스"
        "WALK_ONLY" -> "산책 위주"
        else -> "산책 코스"
    }
}

/**
 * 경유지 정보
 */
data class RoutePlaceInfo(
    val id: Long,
    val name: String,
    val categoryName: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val address: String?,
    val imageUrl: String?
)

/**
 * 좌표 포인트
 */
data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)
