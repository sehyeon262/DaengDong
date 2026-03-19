package com.frontend.ui.screen.walk

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.WalkRoute
import com.frontend.domain.usecase.GetPlacesUseCase
import com.frontend.domain.usecase.ReportDangerZoneUseCase
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kakao.vectormap.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class WalkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reportDangerZoneUseCase: ReportDangerZoneUseCase,
    private val getPlacesUseCase: GetPlacesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WalkState())
    val state = _state.asStateFlow()

    // ── 현재 산책 ID (산책 시작 후 서버에서 발급) ──────────────────────────────
    private var currentWalkId: Long? = null

    // ── 나침반 (방향 센서) ─────────────────────────────────────────────────────
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _azimuth = MutableStateFlow(0f)
    val azimuth = _azimuth.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val newAzimuth = (Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360) % 360
            val current = _azimuth.value
            val diff = abs(newAzimuth - current)
            val normalizedDiff = if (diff > 180f) 360f - diff else diff
            // 5도 이상 변경 시에만 업데이트 (성능 최적화)
            if (normalizedDiff >= 5f) {
                _azimuth.value = newAzimuth
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    init {
        sensorManager.registerListener(
            sensorListener,
            rotationVectorSensor,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    // ── GPS 위치 트래킹 ────────────────────────────────────────────────────────
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _currentPosition = MutableStateFlow<LatLng?>(null)
    val currentPosition = _currentPosition.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _currentPosition.value = LatLng.from(loc.latitude, loc.longitude)
            }
        }
    }

    fun startLocationTracking() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            // 위치 권한 없음
        }
    }

    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(sensorListener)
        stopLocationTracking()
    }

    // ── 산책 경로 ──────────────────────────────────────────────────────────────
    val routes = listOf(
        WalkRoute(
            title = "자유 산책",
            subtitle = "자유롭게 산책해요"
        ),
        WalkRoute(
            title = "공원 & 카페 트레일",
            subtitle = "가볍게 걷기 좋은 길",
            distanceKm = 2.5f,
            durationMin = 40
        ),
        WalkRoute(
            title = "공원 산책로",
            subtitle = "조용한 산책을 즐겨요",
            distanceKm = 1.8f,
            durationMin = 30
        ),
        WalkRoute(
            title = "강변 둘레길",
            subtitle = "탁 트인 뷰를 즐겨요",
            distanceKm = 3.2f,
            durationMin = 55
        )
    )

    fun selectRoute(index: Int) {
        _state.update { it.copy(selectedRouteIndex = index) }
    }

    // ── 필터 ───────────────────────────────────────────────────────────────────

    /** 필터 바텀시트 열기: 현재 activeFilters를 pendingFilters로 복사해 편집 시작 */
    fun showFilter() {
        _state.update { it.copy(showFilterSheet = true, pendingFilters = it.activeFilters) }
    }

    /** 필터 바텀시트 닫기 (변경 사항 버림) */
    fun hideFilter() {
        _state.update { it.copy(showFilterSheet = false, pendingFilters = it.activeFilters) }
    }

    /** 바텀시트에서 필터 항목 토글 (pendingFilters 편집) */
    fun toggleFilter(filter: WalkFilterType) {
        _state.update { state ->
            val updated = if (filter in state.pendingFilters) {
                state.pendingFilters - filter
            } else {
                state.pendingFilters + filter
            }
            state.copy(pendingFilters = updated)
        }
    }

    /** 적용하기: pendingFilters를 activeFilters에 반영하고 필요한 데이터 로드 */
    fun applyFilter() {
        val pending = _state.value.pendingFilters
        val wasPlaceActive = WalkFilterType.PLACE in _state.value.activeFilters
        val isPlaceActive = WalkFilterType.PLACE in pending

        _state.update { it.copy(activeFilters = pending, showFilterSheet = false) }

        // 장소 필터가 꺼진 경우 → 장소 목록 초기화 (마커 제거 + 재조회는 Screen에서 처리)
        if (!isPlaceActive && wasPlaceActive) {
            _state.update { it.copy(places = emptyList()) }
        }
        // 장소 필터 ON 시 로드는 WalkScreen의 LaunchedEffect(activeFilters)에서 지도 중심으로 처리
    }

    /** 지정 좌표 기반 주변 장소 로드 */
    fun loadPlacesByPosition(latitude: Double, longitude: Double) {
        android.util.Log.d("PlaceDebug", "▶ loadPlacesByPosition 호출: lat=$latitude, lon=$longitude")
        viewModelScope.launch {
            _state.update { it.copy(isPlacesLoading = true) }
            getPlacesUseCase(
                latitude = latitude,
                longitude = longitude
            ).onSuccess { places ->
                android.util.Log.d("PlaceDebug", "✅ API 성공: ${places.size}개 장소")
                _state.update { it.copy(places = places, isPlacesLoading = false) }
            }.onFailure {
                android.util.Log.e("PlaceDebug", "❌ API 실패: ${it.javaClass.simpleName} - ${it.message}")
                _state.update { it.copy(isPlacesLoading = false) }
            }
        }
    }

    // ── 산책 ID 관리 ───────────────────────────────────────────────────────────

    /** 산책 시작 후 서버에서 발급된 walkId 저장 */
    fun setCurrentWalkId(walkId: Long) {
        currentWalkId = walkId
    }

    /** 산책 종료 시 walkId 초기화 */
    fun clearCurrentWalkId() {
        currentWalkId = null
    }

    // ── 위험 구역 신고 ─────────────────────────────────────────────────────────

    /** 위치 선택 모드 진입 */
    fun startDangerZoneSelection() {
        _state.update {
            it.copy(
                isSelectingDangerZone = true,
                selectedLocation = null
            )
        }
    }

    /** 위치 선택 모드 취소 */
    fun cancelDangerZoneSelection() {
        _state.update {
            it.copy(
                isSelectingDangerZone = false,
                selectedLocation = null
            )
        }
    }

    /** 지도 중심 좌표를 선택된 위치로 저장 */
    fun selectDangerLocation(location: DangerLocation) {
        _state.update { it.copy(selectedLocation = location) }
    }

    /** 신고 모달 열기 */
    fun openDangerReportDialog() {
        _state.update {
            it.copy(
                isDangerReportDialogOpen = true,
                selectedDangerReason = null,
                customDangerReason = ""
            )
        }
    }

    /** 신고 모달 닫기 */
    fun closeDangerReportDialog() {
        _state.update {
            it.copy(
                isDangerReportDialogOpen = false,
                selectedDangerReason = null,
                customDangerReason = ""
            )
        }
    }

    /** 위험 사유 선택 */
    fun selectDangerReason(reason: DangerReason) {
        _state.update { it.copy(selectedDangerReason = reason) }
    }

    /** "기타" 입력 텍스트 변경 */
    fun updateCustomDangerReason(text: String) {
        _state.update { it.copy(customDangerReason = text) }
    }

    /** 위험 구역 신고 제출 */
    fun submitDangerReport() {
        val location = _state.value.selectedLocation ?: return
        val reason = _state.value.selectedDangerReason ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val customReason = if (reason == DangerReason.OTHER) {
                _state.value.customDangerReason.takeIf { it.isNotBlank() }
            } else null

            try {
                val dangerZone = reportDangerZoneUseCase(
                    walkId = currentWalkId,
                    location = location,
                    reason = reason,
                    customReason = customReason
                ).getOrThrow()

                _state.update {
                    it.copy(
                        dangerZones = it.dangerZones + dangerZone,
                        isSelectingDangerZone = false,
                        selectedLocation = null,
                        isDangerReportDialogOpen = false,
                        selectedDangerReason = null,
                        customDangerReason = "",
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
