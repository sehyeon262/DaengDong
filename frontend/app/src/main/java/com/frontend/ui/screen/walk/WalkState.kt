package com.frontend.ui.screen.walk

data class WalkState(
    val selectedRouteIndex: Int = 0,
    val showFilterSheet: Boolean = false,
    val selectedFilter: WalkFilterType = WalkFilterType.PLACE
)
