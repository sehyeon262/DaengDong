package com.frontend.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 비선호 강아지 알림을 워치 UI에 노출하기 위한 싱글턴 StateFlow */
object DogWarningHolder {
    data class WatchDogWarning(
        val dogName: String = "",
        val distance: Int = 0,
        val isVisible: Boolean = false,
    )

    private val _warning = MutableStateFlow(WatchDogWarning())
    val warning: StateFlow<WatchDogWarning> = _warning.asStateFlow()

    fun show(dogName: String, distance: Int) {
        _warning.value = WatchDogWarning(dogName = dogName, distance = distance, isVisible = true)
    }

    fun dismiss() {
        _warning.value = WatchDogWarning()
    }
}
