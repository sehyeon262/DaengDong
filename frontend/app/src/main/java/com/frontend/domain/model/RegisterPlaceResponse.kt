package com.frontend.domain.model

/** 장소 등록 API (POST /places) 응답 모델 */
data class RegisterPlaceResponse(
    val id: Long,
    val name: String,
    val imageUrl: String?
)
