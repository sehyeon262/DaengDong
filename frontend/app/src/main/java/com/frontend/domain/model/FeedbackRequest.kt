package com.frontend.domain.model

data class FeedbackRequest(
    val targetDogId: Long,
    val myWalkRecordId: Long,
    val feedback: String
)
