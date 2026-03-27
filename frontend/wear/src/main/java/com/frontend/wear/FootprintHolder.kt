package com.frontend.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 수신된 발자국 알림을 워치 UI에 노출하기 위한 싱글턴 StateFlow */
object FootprintHolder {
    data class WatchFootprint(
        val placeId: Long = 0L,
        val placeName: String = "",
        val walkId: Long = 0L,
        val dogId: Long = 0L,
        val isVisible: Boolean = false,
    )

    private val _footprint = MutableStateFlow(WatchFootprint())
    val footprint: StateFlow<WatchFootprint> = _footprint.asStateFlow()

    fun show(placeId: Long, placeName: String, walkId: Long, dogId: Long) {
        _footprint.value = WatchFootprint(placeId, placeName, walkId, dogId, isVisible = true)
    }

    fun dismiss() {
        _footprint.value = WatchFootprint()
    }
}
