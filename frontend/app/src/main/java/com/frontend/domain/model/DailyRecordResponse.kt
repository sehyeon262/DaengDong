package com.frontend.domain.model

data class DailyRecordResponse(
    val date: String,
    val walks: List<WalkItem>
)

data class WalkItem(
    val recordId: Long,
    val startedAt: String,       // "2026-03-12T16:30:00"
    val durationMinutes: Int,
    val distanceKm: Double,
    val thumbnailUrl: String?
)
