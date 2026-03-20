package com.frontend.ui.screen.dog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.local.TokenDataStore
import com.frontend.data.repository.DogRepository
import com.frontend.data.repository.WalkRepository
import com.frontend.domain.model.DogProfileResponse
import com.frontend.domain.model.MetDogResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DogProfileState(
    val isLoading: Boolean = false,
    val profile: DogProfileResponse? = null,
    val recentMetDog: MetDogResponse? = null,
    val dogId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class DogProfileViewModel @Inject constructor(
    private val dogRepository: DogRepository,
    private val walkRepository: WalkRepository,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(DogProfileState())
    val state: StateFlow<DogProfileState> = _state.asStateFlow()

    init {
        loadDogProfile()
    }

    fun loadDogProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val dogId = tokenDataStore.getDogId().first()
                    ?: throw Exception("반려견 정보가 없습니다. 다시 로그인해주세요.")
                val profile = dogRepository.getDogProfile(dogId)
                _state.update { it.copy(isLoading = false, profile = profile, dogId = dogId) }

                walkRepository.getMetDogs(dogId).onSuccess { metDogs ->
                    _state.update { it.copy(recentMetDog = metDogs.firstOrNull()) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "오류가 발생했습니다") }
            }
        }
    }
}
