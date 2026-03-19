package com.frontend.domain.model

data class NearbyDogResponse(
    val dogId: Long,
    val name: String,
    val breed: String,
    val profileImageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceM: Double,
    val walkRecordId: Long,
)

data class NearbyDogsResponse(
    val nearbyDogs: List<NearbyDogResponse>,
    val pendingProposals: List<Any>,
)
