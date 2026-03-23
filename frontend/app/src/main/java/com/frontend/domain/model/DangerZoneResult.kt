package com.frontend.domain.model

data class DangerZoneResult(
    val dangerZone: DangerZone,
    val newBadges: List<NewBadgeInfo> = emptyList()
)

data class DangerZoneApiResponse(
    val riskReportId: Long?,
    val walkSessionId: Long?,
    val latitude: Double?,
    val longitude: Double?,
    val description: String?,
    val createdAt: String?,
    val newBadges: List<NewBadgeInfo>?
)
