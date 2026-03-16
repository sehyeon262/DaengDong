package com.frontend.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun loadHomeData(latitude: Double, longitude: Double, address: String = "현재 위치") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = homeRepository.getHomeData(latitude, longitude)
            result.fold(
                onSuccess = { data ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        data = data,
                        address = address
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "데이터를 불러올 수 없습니다"
                    )
                }
            )
        }
    }
}
