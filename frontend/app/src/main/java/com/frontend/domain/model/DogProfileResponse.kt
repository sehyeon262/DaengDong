package com.frontend.domain.model

data class DogProfileResponse(
    val dogId: Long,
    val name: String,
    val breed: String,
    val birthDate: String,   // "2020-03-08"
    val weight: Double,
    val gender: String       // "MALE" | "FEMALE"
)
