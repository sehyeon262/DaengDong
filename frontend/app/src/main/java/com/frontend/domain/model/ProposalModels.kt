package com.frontend.domain.model

data class SendProposalRequest(
    val fromWalkRecordId: Long,
    val toWalkRecordId: Long,
)

data class RespondProposalRequest(
    val action: String,       // "ACCEPT" | "REJECT"
    val myWalkRecordId: Long,
)
