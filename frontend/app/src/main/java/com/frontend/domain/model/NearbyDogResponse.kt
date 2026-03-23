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
    val feedback: String? = null,  // "좋아요" | "보통" | "싫어요" | null
)

data class PendingProposalInfo(
    val proposalId: String,
    val fromWalkRecordId: Long,
    val dogId: Long,
    val name: String,
    val breed: String,
    val profileImageUrl: String?,
)

data class AcceptedProposalInfo(
    val proposalId: String,
    val dogId: Long,
    val name: String,
    val breed: String,
    val profileImageUrl: String?,
)

data class NearbyDogsResponse(
    val nearbyDogs: List<NearbyDogResponse>,
    val pendingProposals: List<PendingProposalInfo>,
    val acceptedProposals: List<AcceptedProposalInfo>,
)
