package com.frontend.wearable

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 워치→폰 액션을 WalkViewModel에 전달하는 싱글턴 이벤트 버스.
 * PhoneWearableListenerService에서 emit, WalkViewModel에서 collect.
 */
object WearableActionBus {
    private val _actions = MutableSharedFlow<WearableAction>(extraBufferCapacity = 10)
    val actions = _actions.asSharedFlow()

    suspend fun emit(action: WearableAction) {
        _actions.emit(action)
    }
}

sealed class WearableAction {
    data object StartWalk : WearableAction()
    data object EndWalk : WearableAction()
    data object PauseWalk : WearableAction()
    data object ResumeWalk : WearableAction()
    data class ProposalAccept(val proposalId: String, val myWalkRecordId: Long) : WearableAction()
    data class ProposalReject(val proposalId: String, val myWalkRecordId: Long) : WearableAction()
    data class Stamp(val walkId: Long, val dogId: Long, val placeId: Long) : WearableAction()
}
