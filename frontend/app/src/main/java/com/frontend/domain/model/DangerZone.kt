package com.frontend.domain.model

enum class DangerReason(val label: String) {
    FOOT_TRAP("발 빠짐"),
    HAZARDOUS_MATERIAL("유해물질"),
    SNAKE("뱀 출몰"),
    TICK("진드기"),
    OTHER("기타")
}

data class DangerLocation(
    val latitude: Double,
    val longitude: Double
)

data class DangerZone(
    val id: Long,
    val location: DangerLocation,
    val reason: DangerReason,
    val customReason: String? = null
)

/** 백엔드 RiskReportResponse와 매핑되는 서버 응답 DTO */
data class RiskReportData(
    val riskReportId: Long,
    val walkSessionId: Long?,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val createdAt: String
)

/**
 * 백엔드 NearbyRiskReportResponse와 매핑되는 서버 응답 DTO
 * - GET /api/v1/safety/risk-zones/nearby 응답용
 * - distanceM: 현재 위치에서 위험 구역까지의 거리 (미터)
 */
data class NearbyRiskReportData(
    val riskReportId: Long,
    val walkSessionId: Long?,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val createdAt: String,
    val distanceM: Double
)

/**
 * 개인 위험장소 (서버에 영구 저장된 상태)
 * - mine API 응답 기반
 * - isPersisted: 서버에 저장 완료 여부
 */
data class PersistedDangerZone(
    val id: Long,
    val location: DangerLocation,
    val reason: DangerReason,
    val customReason: String? = null,
    val createdAt: String? = null
)

/**
 * 근접 위험장소 (nearby API 응답 기반)
 * - 알림 트리거 용도
 * - distanceM: 현재 위치에서의 거리 (미터)
 */
data class NearbyDangerZone(
    val id: Long,
    val location: DangerLocation,
    val reason: DangerReason,
    val customReason: String? = null,
    val distanceM: Double
)
