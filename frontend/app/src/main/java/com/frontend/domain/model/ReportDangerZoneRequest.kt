package com.frontend.domain.model

data class ReportDangerZoneRequest(
    val walkId: Long?,
    val latitude: Double,
    val longitude: Double,
    val reason: String,
    val customReason: String?
)
