package com.frontend.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 폰→워치로 전달된 산책 통계를 보관하는 싱글턴 StateFlow */
object WalkStatsHolder {
    private val _stats = MutableStateFlow(WatchWalkStats())
    val stats: StateFlow<WatchWalkStats> = _stats.asStateFlow()

    fun update(
        elapsedSeconds: Int,
        distanceMeters: Double,
        calories: Int,
        isWalking: Boolean,
        isPaused: Boolean,
    ) {
        _stats.update {
            it.copy(
                elapsedSeconds = elapsedSeconds,
                distanceMeters = distanceMeters,
                calories = calories,
                isWalking = isWalking,
                isPaused = isPaused,
            )
        }
    }

    fun reset() {
        _stats.value = WatchWalkStats()
    }
}

data class WatchWalkStats(
    val elapsedSeconds: Int = 0,
    val distanceMeters: Double = 0.0,
    val calories: Int = 0,
    val isWalking: Boolean = false,
    val isPaused: Boolean = false,
)
