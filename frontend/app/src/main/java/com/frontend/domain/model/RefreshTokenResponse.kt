package com.frontend.domain.model

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)
