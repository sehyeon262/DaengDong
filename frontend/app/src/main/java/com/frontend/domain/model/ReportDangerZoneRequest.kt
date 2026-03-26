package com.frontend.domain.model

/** 백엔드 CreateRiskReportRequest와 필드명 일치 */
data class ReportDangerZoneRequest(
    val walkSessionId: Long?,
    val latitude: Double,
    val longitude: Double,
    val description: String
)
