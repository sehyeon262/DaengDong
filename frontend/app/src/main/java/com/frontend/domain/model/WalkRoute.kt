package com.frontend.domain.model

data class WalkRoute(
    val title: String,
    val subtitle: String,
    val distanceKm: Float? = null,
    val durationMin: Int? = null
)
