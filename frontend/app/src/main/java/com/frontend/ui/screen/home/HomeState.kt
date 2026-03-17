package com.frontend.ui.screen.home

import com.frontend.domain.model.HomeResponse

data class HomeState(
    val isLoading: Boolean = true,
    val data: HomeResponse? = null,
    val error: String? = null,
    val address: String = "현재 위치"
)
