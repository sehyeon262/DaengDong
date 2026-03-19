package com.frontend.domain.model

data class CalendarResponse(
    val year: Int,
    val month: Int,
    val summary: CalendarSummary,
    val days: List<DayWalkCount>
)

data class CalendarSummary(
    val walkRate: Int,
    val totalDurationMinutes: Int,
    val totalDistanceKm: Double
)

data class DayWalkCount(
    val date: String,   // "2026-03-12"
    val walkCount: Int
)
