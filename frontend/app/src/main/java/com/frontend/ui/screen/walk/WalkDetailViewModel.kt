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
    val error: String? = null,
    val isPhotoUploading: Boolean = false,
    val photoError: String? = null
)

@HiltViewModel
class WalkDetailViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val walkId: Long = savedStateHandle.get<Long>("walkId") ?: 0L

    private val _state = MutableStateFlow(WalkDetailState())
    val state: StateFlow<WalkDetailState> = _state.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    init {
        loadWalkDetail()
    }

    fun loadWalkDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            walkRepository.getWalkDetail(walkId)
                .onSuccess { detail ->
                    _state.update { it.copy(isLoading = false, detail = detail) }
                    startPollingIfNeeded(detail)
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "오류가 발생했습니다") }
                }
        }
    }

    // diary가 생성 중(content=null)이면 5초마다 재조회
    private fun startPollingIfNeeded(detail: WalkDetailResponse) {
        val isGenerating = detail.diary != null && detail.diary.content == null
        if (!isGenerating) return

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                walkRepository.getWalkDetail(walkId).onSuccess { newDetail ->
                    _state.update { it.copy(detail = newDetail) }
                    if (newDetail.diary?.content != null) {
                        pollingJob?.cancel() // 완성되면 폴링 중단
                    }
                }
            }
        }
    }

    /** 사진 삭제 */
    fun deletePhoto(photoUrl: String) {
        viewModelScope.launch {
            walkRepository.deletePhoto(walkId, photoUrl).onSuccess { updatedUrls ->
                _state.update { it.copy(
                    detail = it.detail?.copy(photoUrls = updatedUrls)
                )}
            }.onFailure { e ->
                _state.update { it.copy(photoError = e.message) }
            }
        }
    }

    /** 사진 업로드 */
    fun uploadPhotos(parts: List<okhttp3.MultipartBody.Part>) {
        viewModelScope.launch {
            _state.update { it.copy(isPhotoUploading = true) }
            walkRepository.uploadPhotos(walkId, parts).onSuccess { updatedUrls ->
                _state.update { it.copy(
                    isPhotoUploading = false,
                    detail = it.detail?.copy(photoUrls = updatedUrls)
                )}
            }.onFailure { e ->
                _state.update { it.copy(isPhotoUploading = false, photoError = e.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
