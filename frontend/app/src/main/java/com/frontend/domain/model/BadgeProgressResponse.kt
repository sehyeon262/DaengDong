package com.frontend.domain.model

data class BadgeProgressResponse(
    val badgeId: Long,
    val badgeName: String,
    val description: String,
    val currentValue: Int,
    val targetValue: Int,
    val earned: Boolean
)
