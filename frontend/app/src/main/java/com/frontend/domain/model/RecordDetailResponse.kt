package com.frontend.domain.model

data class RecordDetailResponse(
    val recordId: Long,
    val startedAt: String,
    val endedAt: String?,
    val durationMinutes: Int,
    val distanceKm: Double,
    val calories: Double,
    val photoUrls: List<String>,
)
