package com.frontend.ui.screen.walk

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.repository.WalkRepository
import com.frontend.domain.model.WalkDetailResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalkDetailState(
    val isLoading: Boolean = false,
    val detail: WalkDetailResponse? = null,
    val error: String? = null
)

@HiltViewModel
class WalkDetailViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val walkId: Long = savedStateHandle.get<Long>("walkId") ?: 0L

    private val _state = MutableStateFlow(WalkDetailState())
    val state: StateFlow<WalkDetailState> = _state.asStateFlow()

    init {
        loadWalkDetail()
    }

    fun loadWalkDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            walkRepository.getWalkDetail(walkId)
                .onSuccess { detail ->
                    _state.update { it.copy(isLoading = false, detail = detail) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "오류가 발생했습니다") }
                }
        }
    }
}
