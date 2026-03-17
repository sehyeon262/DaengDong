package com.frontend.domain.model

data class StartWalkResponse(
    val walkId: Long,
    val status: String,  // "IN_PROGRESS" 등
)
