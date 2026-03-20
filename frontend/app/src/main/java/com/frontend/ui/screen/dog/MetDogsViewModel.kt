package com.frontend.ui.screen.dog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.repository.WalkRepository
import com.frontend.domain.model.FeedbackRequest
import com.frontend.domain.model.MetDogResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetDogsState(
    val isLoading: Boolean = false,
    val allMetDogs: List<MetDogResponse> = emptyList(),
    val recentMetDogs: List<MetDogResponse> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MetDogsViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dogId: Long = savedStateHandle.get<Long>("dogId") ?: 0L

    private val _state = MutableStateFlow(MetDogsState())
    val state: StateFlow<MetDogsState> = _state.asStateFlow()

    init {
        loadMetDogs()
    }

    fun loadMetDogs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            walkRepository.getMetDogs(dogId)
                .onSuccess { all ->
                    // "최근 만남" = 가장 최근 walkRecordId와 동일한 항목들
                    val latestWalkRecordId = all.firstOrNull()?.lastWalkRecordId
                    val recent = if (latestWalkRecordId != null) {
                        all.filter { it.lastWalkRecordId == latestWalkRecordId }
                    } else {
                        emptyList()
                    }
                    _state.update {
                        it.copy(isLoading = false, allMetDogs = all, recentMetDogs = recent)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "오류가 발생했습니다")
                    }
                }
        }
    }

    fun updateFeedback(targetDogId: Long, walkRecordId: Long, feedback: String) {
        viewModelScope.launch {
            walkRepository.updateFeedback(
                FeedbackRequest(
                    targetDogId = targetDogId,
                    myWalkRecordId = walkRecordId,
                    feedback = feedback
                )
            ).onSuccess {
                // 로컬 상태 즉시 업데이트
                _state.update { state ->
                    val updatedAll = state.allMetDogs.map { dog ->
                        if (dog.targetDogId == targetDogId) {
                            dog.copy(feedback = feedback, feedbackDone = feedback != "보통")
                        } else dog
                    }
                    val updatedRecent = state.recentMetDogs.map { dog ->
                        if (dog.targetDogId == targetDogId) {
                            dog.copy(feedback = feedback, feedbackDone = feedback != "보통")
                        } else dog
                    }
                    state.copy(allMetDogs = updatedAll, recentMetDogs = updatedRecent)
                }
            }
        }
    }
}
