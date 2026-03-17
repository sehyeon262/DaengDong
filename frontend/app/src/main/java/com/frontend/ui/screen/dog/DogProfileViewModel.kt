package com.frontend.ui.screen.dog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.repository.DogRepository
import com.frontend.domain.model.DogProfileResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DogProfileState(
    val isLoading: Boolean = false,
    val profile: DogProfileResponse? = null,
    val error: String? = null
)

@HiltViewModel
class DogProfileViewModel @Inject constructor(
    private val dogRepository: DogRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DogProfileState())
    val state: StateFlow<DogProfileState> = _state.asStateFlow()

    init {
        loadDogProfile()
    }

    fun loadDogProfile(dogId: Long = 1L) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val profile = dogRepository.getDogProfile(dogId)
                _state.update { it.copy(isLoading = false, profile = profile) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "오류가 발생했습니다") }
            }
        }
    }
}
