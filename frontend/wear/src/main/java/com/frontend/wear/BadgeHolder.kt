package com.frontend.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 배지 획득 알림을 워치 UI에 노출하기 위한 싱글턴 StateFlow */
object BadgeHolder {
    data class WatchBadge(
        val badgeId: Long = 1L,
        val badgeName: String = "",
        val isVisible: Boolean = false,
    )

    private val _badge = MutableStateFlow(WatchBadge())
    val badge: StateFlow<WatchBadge> = _badge.asStateFlow()

    fun show(badgeId: Long, badgeName: String) {
        _badge.value = WatchBadge(badgeId = badgeId, badgeName = badgeName, isVisible = true)
    }

    fun dismiss() {
        _badge.value = WatchBadge()
    }
}
