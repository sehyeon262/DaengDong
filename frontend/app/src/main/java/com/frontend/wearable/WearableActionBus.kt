package com.frontend.wearable

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 워치→폰 액션을 WalkViewModel에 전달하는 싱글턴 이벤트 버스.
 * PhoneWearableListenerService에서 emit, WalkViewModel에서 collect.
 *
 * SharedFlow(replay=0)는 새 collector에게 과거 이벤트를 전달하지 않으므로,
 * WalkViewModel 생성 전에 도착한 StartWalk/SelectCourse는 StateFlow로 별도 보관.
 *
 * 중복 emit 방지: PhoneWearableListenerService(백그라운드)와 WearableManager.foregroundListener(포그라운드)가
 * 동시에 같은 메시지를 처리하면 액션이 두 번 emit되어 네비게이션이 두 번 실행됨.
 * 1초 이내 동일 액션은 중복으로 간주해 무시한다.
 */
object WearableActionBus {
    private val _actions = MutableSharedFlow<WearableAction>(extraBufferCapacity = 10)
    val actions = _actions.asSharedFlow()

    // WalkViewModel 생성 전에 StartWalk가 도착한 경우를 위한 플래그
    private val _pendingStartWalk = MutableStateFlow(false)
    val pendingStartWalk: StateFlow<Boolean> = _pendingStartWalk.asStateFlow()

    // WalkViewModel 생성 전에 SelectCourse가 도착한 경우를 위한 인덱스
    private val _pendingCourseIndex = MutableStateFlow<Int?>(null)
    val pendingCourseIndex: StateFlow<Int?> = _pendingCourseIndex.asStateFlow()

    // 중복 emit 방지용
    private var lastAction: WearableAction? = null
    private var lastActionTime: Long = 0L
    private const val DEDUP_WINDOW_MS = 1000L

    suspend fun emit(action: WearableAction) {
        val now = System.currentTimeMillis()
        if (action == lastAction && now - lastActionTime < DEDUP_WINDOW_MS) return
        lastAction = action
        lastActionTime = now

        when (action) {
            is WearableAction.StartWalk -> _pendingStartWalk.value = true
            is WearableAction.SelectCourse -> _pendingCourseIndex.value = action.courseIndex
            else -> {}
        }
        _actions.emit(action)
    }

    fun consumePendingStartWalk() {
        _pendingStartWalk.value = false
    }

    fun consumePendingCourseIndex() {
        _pendingCourseIndex.value = null
    }
}

sealed class WearableAction {
    data object StartWalk : WearableAction()
    data object EndWalk : WearableAction()
    data object PauseWalk : WearableAction()
    data object ResumeWalk : WearableAction()
    data class SelectCourse(val courseIndex: Int) : WearableAction()
    data class ProposalAccept(val proposalId: String, val myWalkRecordId: Long) : WearableAction()
    data class ProposalReject(val proposalId: String, val myWalkRecordId: Long) : WearableAction()
    data class Stamp(val walkId: Long, val dogId: Long, val placeId: Long) : WearableAction()
    data object DismissWalkSummary : WearableAction()
}
