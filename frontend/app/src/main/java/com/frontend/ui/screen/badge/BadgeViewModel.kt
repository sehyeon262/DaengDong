package com.frontend.ui.screen.badge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.repository.BadgeRepository
import com.frontend.domain.model.BadgeProgressResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BadgeState(
    val isLoading: Boolean = false,
    val badges: List<BadgeProgressResponse> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class BadgeViewModel @Inject constructor(
    private val badgeRepository: BadgeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BadgeState())
    val state: StateFlow<BadgeState> = _state.asStateFlow()

    init {
        loadBadges()
    }

    fun loadBadges() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val badges = badgeRepository.getBadgeProgress()
                _state.update { it.copy(isLoading = false, badges = badges) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "오류가 발생했습니다") }
            }
        }
    }
}
