package com.frontend.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WatchCourse(
    val index: Int,
    val type: String,
    val name: String,
    val distanceKm: Double,
    val durationMin: Int,
)

/** 폰에서 전달받은 코스 목록을 보관하는 싱글턴 StateFlow */
object CoursesHolder {
    private val _courses = MutableStateFlow(
        listOf(WatchCourse(0, "FREE", "자유 산책", 0.0, 0))
    )
    val courses: StateFlow<List<WatchCourse>> = _courses.asStateFlow()

    fun update(courses: List<WatchCourse>) {
        _courses.value = courses
    }
}
