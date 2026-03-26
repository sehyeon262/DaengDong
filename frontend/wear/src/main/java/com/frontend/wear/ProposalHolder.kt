package com.frontend.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 수신된 산책 제안을 워치 UI에 노출하기 위한 싱글턴 StateFlow */
object ProposalHolder {
    data class WatchProposal(
        val proposalId: String = "",
        val dogName: String = "",
        val breed: String = "",
        val myWalkRecordId: Long = 0L,
        val isVisible: Boolean = false,
    )

    private val _proposal = MutableStateFlow(WatchProposal())
    val proposal: StateFlow<WatchProposal> = _proposal.asStateFlow()

    fun show(proposalId: String, dogName: String, breed: String, myWalkRecordId: Long) {
        _proposal.value = WatchProposal(proposalId, dogName, breed, myWalkRecordId, isVisible = true)
    }

    fun dismiss() {
        _proposal.value = WatchProposal()
    }
}
