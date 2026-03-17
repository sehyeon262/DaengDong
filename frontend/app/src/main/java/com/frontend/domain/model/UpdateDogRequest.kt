package com.frontend.domain.model

data class UpdateDogRequest(
    val name: String,
    val breed: String,
    val birthDate: String,
    val gender: String
)
