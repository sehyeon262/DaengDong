package com.frontend.domain.model

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long
)