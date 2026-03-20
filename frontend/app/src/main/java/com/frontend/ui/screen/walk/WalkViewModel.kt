package com.frontend.ui.screen.walk

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.repository.WalkRepository
import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.LocationBatchRequest
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@HiltViewModel
class WalkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reportDangerZoneUseCase: ReportDangerZoneUseCase,
    private val getPlacesUseCase: GetPlacesUseCase,
    private val walkRepository: WalkRepository,
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

    // ── 산책 경로 (실시간 폴리라인용) ─────────────────────────────────────────
    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints = _routePoints.asStateFlow()

    // ── GPS 배치 전송 대기열 (10초마다 서버로 전송) ───────────────────────────
    private val pendingPoints = mutableListOf<LocationBatchRequest.LocationPoint>()
    private var batchSendJob: Job? = null
    private var timerJob: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                val latLng = LatLng.from(loc.latitude, loc.longitude)
                val prev = _currentPosition.value
                _currentPosition.value = latLng

                // 산책 중이고 일시정지가 아닐 때만 경로/거리 누적
                if (_state.value.isWalking && !_state.value.isPaused) {
                    if (prev != null) {
                        val d = haversineMeters(prev.latitude, prev.longitude, loc.latitude, loc.longitude)
                        _state.update { it.copy(distanceMeters = it.distanceMeters + d) }
                    }
                    _routePoints.value = _routePoints.value + latLng
                    pendingPoints.add(
                        LocationBatchRequest.LocationPoint(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            timestamp = System.currentTimeMillis(),
                        )
                    )
                }
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
        batchSendJob?.cancel()
        timerJob?.cancel()
    }

    // ── 산책 경로 카드 ─────────────────────────────────────────────────────────
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
        viewModelScope.launch {
            _state.update { it.copy(isPlacesLoading = true) }
            getPlacesUseCase(
                latitude = latitude,
                longitude = longitude
            ).onSuccess { places ->
                _state.update { it.copy(places = places, isPlacesLoading = false) }
            }.onFailure {
                _state.update { it.copy(isPlacesLoading = false) }
            }
        }
    }

    // ── 자유 산책 시작 ─────────────────────────────────────────────────────────

    /**
     * R1-03: 자유 산책 시작
     * - 버튼 클릭 즉시 isWalking = true (낙관적 UI 전환 → 즉각 화면 전환)
     * - 타이머 즉시 시작
     * - 백그라운드에서 서버 요청 → walkId 수신 후 GPS 배치 전송 시작
     */
    fun startFreeWalk() {
        // 즉시 UI 전환
        _state.update {
            it.copy(
                isWalking = true,
                isPaused = false,
                elapsedSeconds = 0,
                distanceMeters = 0.0,
                walkError = null,
            )
        }
        _routePoints.value = emptyList()
        pendingPoints.clear()
        startWalkTimer()

        // 백그라운드 서버 요청
        viewModelScope.launch {
            walkRepository.startFreeWalk()
                .onSuccess { walkId ->
                    currentWalkId = walkId
                    startBatchSending(walkId)
                }
                .onFailure { e ->
                    // 서버 실패해도 UI는 산책 중 상태 유지 (GPS 배치만 못 보냄)
                    android.util.Log.w("WalkViewModel", "산책 시작 API 실패: ${e.message}")
                    _state.update { it.copy(walkError = e.message) }
                }
        }
    }

    /**
     * W1-06: 산책 종료
     * - 타이머 정지
     * - 남은 GPS 포인트 서버 전송
     * - 요약 화면 표시 (routePoints는 요약 화면에서 지도 표시용으로 유지)
     */
    fun endWalk() {
        timerJob?.cancel()
        timerJob = null

        // 요약 데이터 캡처 (상태 초기화 전)
        val summarySeconds = _state.value.elapsedSeconds
        val summaryDistance = _state.value.distanceMeters
        val summaryRoute = routes.getOrNull(_state.value.selectedRouteIndex)?.title ?: "자유 산책"

        val walkId = currentWalkId
        if (walkId == null) {
            // 서버 walkId 없이 종료 (API 실패 케이스)
            batchSendJob?.cancel()
            pendingPoints.clear()
            _state.update {
                it.copy(
                    isWalking = false,
                    isPaused = false,
                    elapsedSeconds = 0,
                    distanceMeters = 0.0,
                    walkError = null,
                    isWalkSummaryVisible = true,
                    summaryElapsedSeconds = summarySeconds,
                    summaryDistanceMeters = summaryDistance,
                    summaryRouteName = summaryRoute,
                    summaryRating = 0,
                )
            }
            return
        }

        viewModelScope.launch {
            batchSendJob?.cancel()

            // 남은 포인트 전송 (2개 미만이면 건너뜀 - 백엔드 LINESTRING 최소 2점 필요)
            val remaining = pendingPoints.toList()
            pendingPoints.clear()
            if (remaining.size >= 2) {
                walkRepository.saveLocations(walkId, remaining)
            }

            walkRepository.endWalk(walkId)
                .onSuccess {
                    currentWalkId = null
                    // routePoints는 유지 → 요약 화면 배경 지도에 경로 표시
                    _state.update {
                        it.copy(
                            isWalking = false,
                            isPaused = false,
                            elapsedSeconds = 0,
                            distanceMeters = 0.0,
                            walkError = null,
                            isWalkSummaryVisible = true,
                            summaryElapsedSeconds = summarySeconds,
                            summaryDistanceMeters = summaryDistance,
                            summaryRouteName = summaryRoute,
                            summaryRating = 0,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(walkError = e.message) }
                }
        }
    }

    /** 산책 별점 선택 */
    fun setWalkRating(rating: Int) {
        _state.update { it.copy(summaryRating = rating) }
    }

    /** 요약 화면 닫기 — routePoints 초기화 */
    fun dismissWalkSummary() {
        _routePoints.value = emptyList()
        _state.update {
            it.copy(
                isWalkSummaryVisible = false,
                summaryElapsedSeconds = 0,
                summaryDistanceMeters = 0.0,
                summaryRouteName = "",
                summaryRating = 0,
            )
        }
    }

    /** 일시정지 */
    fun pauseWalk() {
        _state.update { it.copy(isPaused = true) }
    }

    /** 산책 재개 */
    fun resumeWalk() {
        _state.update { it.copy(isPaused = false) }
    }

    /** 1초마다 elapsedSeconds 증가 (일시정지 중에는 멈춤) */
    private fun startWalkTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (_state.value.isWalking && !_state.value.isPaused) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    /**
     * 10초마다 pending 포인트를 서버로 배치 전송
     * - 2개 미만이면 건너뜀 (LINESTRING 최소 2점 필요)
     */
    private fun startBatchSending(walkId: Long) {
        batchSendJob?.cancel()
        batchSendJob = viewModelScope.launch {
            while (true) {
                delay(10_000L)
                val batch = pendingPoints.toList()
                if (batch.size >= 2) {
                    pendingPoints.clear()
                    walkRepository.saveLocations(walkId, batch)
                }
            }
        }
    }

    /** Haversine 공식 — 두 좌표 사이 거리 (미터) */
    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)
        val a = sin(Δφ / 2) * sin(Δφ / 2) + cos(φ1) * cos(φ2) * sin(Δλ / 2) * sin(Δλ / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
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
