package com.frontend.domain.model

data class MetDogResponse(
    val metDogId: Long,
    val targetDogId: Long,
    val targetDogName: String,
    val targetDogBreed: String?,
    val targetDogProfileImageUrl: String?,
    val feedback: String,
    val feedbackDone: Boolean,
    val lastMetAt: String,
    val lastWalkRecordId: Long
)
