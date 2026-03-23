package com.frontend.domain.model

data class EndWalkResponse(
    val walkId: Long,
    val endTime: String?,
    val totalDuration: Int?,
    val totalDistance: Double?,
    val calories: Double?,
    val newBadges: List<NewBadgeInfo>?
)

data class NewBadgeInfo(
    val badgeId: Long,
    val badgeName: String,
    val description: String,
    val conditionValue: String?
)
