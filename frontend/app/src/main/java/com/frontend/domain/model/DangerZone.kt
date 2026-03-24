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
