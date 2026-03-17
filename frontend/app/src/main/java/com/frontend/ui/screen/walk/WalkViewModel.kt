package com.frontend.ui.screen.walk

import androidx.lifecycle.ViewModel
import com.frontend.domain.model.WalkRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class WalkViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(WalkState())
    val state = _state.asStateFlow()

    val routes = listOf(
        WalkRoute(
            title = "자유 산책",
            subtitle = "자유롭게 산책해요"
        ),
        WalkRoute(
            title = "공원 & 카페 트레일",
            subtitle = "가볍게 걷기 좋은 길",
            distanceKm = 2.5f,
            durationMin = 40
        ),
        WalkRoute(
            title = "공원 산책로",
            subtitle = "조용한 산책을 즐겨요",
            distanceKm = 1.8f,
            durationMin = 30
        ),
        WalkRoute(
            title = "강변 둘레길",
            subtitle = "탁 트인 뷰를 즐겨요",
            distanceKm = 3.2f,
            durationMin = 55
        )
    )

    fun selectRoute(index: Int) {
        _state.update { it.copy(selectedRouteIndex = index) }
    }

    fun showFilter() {
        _state.update { it.copy(showFilterSheet = true) }
    }

    fun hideFilter() {
        _state.update { it.copy(showFilterSheet = false) }
    }

    fun selectFilter(filter: WalkFilterType) {
        _state.update { it.copy(selectedFilter = filter) }
    }

    fun applyFilter() {
        _state.update { it.copy(showFilterSheet = false) }
    }
}
