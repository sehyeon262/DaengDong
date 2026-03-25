package com.frontend.domain.model

data class Place(
    val id: Long,
    val name: String,
    val categoryName: String? = null,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val contact: String? = null,
    val imageUrl: String?,
    // 주변 장소 조회 시 서버가 계산해서 내려주는 거리 (미터 단위)
    val distanceMeters: Double? = null,
    // 상세 조회 시에만 내려오는 장소 설명
    val description: String? = null
)
