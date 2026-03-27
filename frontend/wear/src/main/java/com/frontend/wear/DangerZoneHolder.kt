package com.frontend.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 위험 구역 알림을 워치 UI에 노출하기 위한 싱글턴 StateFlow */
object DangerZoneHolder {
    data class WatchDangerZone(
        val reason: String = "",
        val isVisible: Boolean = false,
    )

    private val _danger = MutableStateFlow(WatchDangerZone())
    val danger: StateFlow<WatchDangerZone> = _danger.asStateFlow()

    fun show(reason: String) {
        _danger.value = WatchDangerZone(reason = reason, isVisible = true)
    }

    fun dismiss() {
        _danger.value = WatchDangerZone()
    }
}
