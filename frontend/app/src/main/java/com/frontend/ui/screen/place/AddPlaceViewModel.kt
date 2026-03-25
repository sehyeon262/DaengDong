package com.frontend.ui.screen.place

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.BuildConfig
import com.frontend.data.remote.KakaoLocalApi
import com.frontend.data.repository.PlaceRepository
import com.frontend.domain.model.KakaoPlace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddPlaceUiState(
    val name: String = "",
    val searchQuery: String = "",           // 검색 다이얼로그 내부 입력값
    val searchResults: List<KakaoPlace> = emptyList(),
    val isSearching: Boolean = false,
    val selectedAddress: String = "",       // "주소 찾기" 칸에 표시되는 확정 주소
    val detailAddress: String = "",         // 상세주소 입력 칸
    val isAddressSearchOpen: Boolean = false, // 주소 검색 다이얼로그 열림 여부
    val latitude: Double = 37.5665,         // 기본값: 서울 시청
    val longitude: Double = 126.9780,
    val selectedCategory: String? = null,
    val memo: String = "",
    val imageUri: Uri? = null,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddPlaceViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val kakaoLocalApi: KakaoLocalApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPlaceUiState())
    val uiState: StateFlow<AddPlaceUiState> = _uiState.asStateFlow()

    private val _searchQueryFlow = MutableStateFlow("")

    init {
        observeSearchQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQueryFlow
                .debounce(400)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query -> searchAddress(query) }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onSearchQueryChange(value: String) {
        _uiState.update { it.copy(searchQuery = value, searchResults = emptyList()) }
        _searchQueryFlow.value = value
    }

    fun onCategorySelect(category: String) {
        val newValue = if (_uiState.value.selectedCategory == category) null else category
        _uiState.update { it.copy(selectedCategory = newValue) }
    }

    fun onMemoChange(value: String) {
        _uiState.update { it.copy(memo = value) }
    }

    fun onDetailAddressChange(value: String) {
        _uiState.update { it.copy(detailAddress = value) }
    }

    fun openAddressSearch() {
        _uiState.update { it.copy(isAddressSearchOpen = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeAddressSearch() {
        _uiState.update { it.copy(isAddressSearchOpen = false, searchQuery = "", searchResults = emptyList()) }
    }

    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(imageUri = uri) }
    }

    fun onImageRemoved() {
        _uiState.update { it.copy(imageUri = null) }
    }

    /** 화면 진입 시 GPS로 가져온 현재 위치를 지도 초기 좌표로 설정하고 역지오코딩으로 주소 자동 입력 */
    fun onCurrentLocationObtained(lat: Double, lon: Double) {
        _uiState.update { it.copy(latitude = lat, longitude = lon) }
        reverseGeocode(lat, lon)
    }

    /** 사용자가 지도를 탭해서 위치를 직접 선택한 경우 — 역지오코딩으로 "주소 찾기" 칸 자동 갱신 */
    fun onMapTapped(lat: Double, lon: Double) {
        _uiState.update { it.copy(latitude = lat, longitude = lon, searchResults = emptyList()) }
        reverseGeocode(lat, lon)
    }

    /** 좌표 → 주소 변환 (Kakao Coord2Address API) */
    private fun reverseGeocode(lat: Double, lon: Double) {
        viewModelScope.launch {
            runCatching {
                kakaoLocalApi.coord2Address(
                    authorization = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}",
                    longitude = lon,
                    latitude = lat
                )
            }.onSuccess { response ->
                val doc = response.documents.firstOrNull()
                val address = doc?.roadAddress?.addressName
                    ?: doc?.address?.addressName
                    ?: ""
                if (address.isNotEmpty()) {
                    _uiState.update { it.copy(selectedAddress = address) }
                }
            }
            // 실패 시 무시 — 주소 칸이 빈 채로 유지됨
        }
    }

    fun onPlaceSelected(place: KakaoPlace) {
        val address = place.roadAddressName.ifEmpty { place.addressName }
        _uiState.update {
            it.copy(
                selectedAddress = address,
                searchQuery = "",
                isAddressSearchOpen = false,   // 선택 즉시 다이얼로그 닫기
                latitude = place.y.toDoubleOrNull() ?: it.latitude,
                longitude = place.x.toDoubleOrNull() ?: it.longitude,
                searchResults = emptyList()
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun searchAddress(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            runCatching {
                kakaoLocalApi.searchKeyword(
                    authorization = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}",
                    query = query
                )
            }.onSuccess { response ->
                _uiState.update { it.copy(searchResults = response.documents, isSearching = false) }
            }.onFailure {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun registerPlace(context: Context) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "장소명을 입력해주세요") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            // 주소 = 도로명 주소 + 상세주소 조합
            val fullAddress = listOf(state.selectedAddress, state.detailAddress)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifEmpty { null }
            placeRepository.registerPlace(
                context = context,
                name = state.name.trim(),
                categoryName = state.selectedCategory,
                latitude = state.latitude,
                longitude = state.longitude,
                address = fullAddress,
                memo = state.memo.ifEmpty { null },
                imageUri = state.imageUri
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false, isSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "등록에 실패했습니다") }
            }
        }
    }
}
