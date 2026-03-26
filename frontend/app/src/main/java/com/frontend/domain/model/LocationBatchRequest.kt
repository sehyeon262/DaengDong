package com.frontend.domain.model

/**
 * POST /walks/{walkId}/locations 요청 바디
 * 백엔드가 기대하는 배치 형식
 */
data class LocationBatchRequest(
    val locations: List<LocationPoint>
) {
    data class LocationPoint(
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long,
    )
}
