package com.frontend.ui.screen.walk

import android.content.Context
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.frontend.data.local.TokenDataStore
import com.frontend.data.repository.DogRepository
import com.frontend.data.repository.WalkRepository
import com.frontend.domain.model.FeedbackRequest
import com.frontend.domain.model.NearbyDogResponse
import com.frontend.domain.model.PendingProposalInfo
import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.LocationBatchRequest
import com.frontend.domain.model.Place
import com.frontend.domain.model.RecommendedRoute
import com.frontend.domain.model.WalkRoute
import com.frontend.domain.usecase.EndWalkUseCase
import com.frontend.domain.usecase.GetDangerZonesUseCase
import com.frontend.domain.usecase.GetPlaceDetailUseCase
import com.frontend.domain.usecase.GetPlacesUseCase
import com.frontend.domain.usecase.GetRecommendedRoutesUseCase
import com.frontend.domain.usecase.ReportDangerZoneUseCase
import com.frontend.domain.usecase.SaveLocationsUseCase
import com.frontend.domain.usecase.StartFreeWalkUseCase
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kakao.vectormap.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val getDangerZonesUseCase: GetDangerZonesUseCase,
    private val getPlacesUseCase: GetPlacesUseCase,
    private val walkRepository: WalkRepository,
    private val tokenDataStore: TokenDataStore,
    private val dogRepository: DogRepository,
    private val getPlaceDetailUseCase: GetPlaceDetailUseCase,
    private val getRecommendedRoutesUseCase: GetRecommendedRoutesUseCase,
    private val startFreeWalkUseCase: StartFreeWalkUseCase,
    private val saveLocationsUseCase: SaveLocationsUseCase,
    private val endWalkUseCase: EndWalkUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WalkState())
    val state = _state.asStateFlow()

    // ── 주변 강아지 폴링 Job ───────────────────────────────────────────────────
    private var nearbyDogsJob: Job? = null

    // ── 현재 산책 ID (산책 시작 후 서버에서 발급) ──────────────────────────────
    private var currentWalkId: Long? = null
    // 산책 시작 API 응답을 기다리기 위한 Deferred (endWalk에서 대기 가능)
    private var walkIdDeferred: CompletableDeferred<Long?>? = null

    // ── 위험구역 최초 로드 여부 (위치 수신 후 1회만 로드) ───────────────────────
    private var dangerZonesLoaded = false

    // ── 산책 경로 포인트 (지도 경로 표시용) ────────────────────────────────────
    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints = _routePoints.asStateFlow()

    // ── GPS 배치 전송 버퍼 ──────────────────────────────────────────────────────
    private val pendingPoints = mutableListOf<LocationBatchRequest.LocationPoint>()

    // ── 타이머 / 배치 전송 Job ──────────────────────────────────────────────────
    private var timerJob: Job? = null
    private var batchSendJob: Job? = null

    // ── 카메라 사진 자동 감지 (산책 중 촬영 사진 자동 업로드) ────────────────────
    private var photoObserver: ContentObserver? = null
    private var walkStartTimestamp: Long = 0L
    private val uploadedPhotoIds = mutableSetOf<Long>()

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
        // DataStore에서 myDogId 초기 로드
        viewModelScope.launch {
            tokenDataStore.getDogId().first()?.let { dogId ->
                _state.update { it.copy(myDogId = dogId) }
            }
        }
    }

    // ── GPS 위치 트래킹 ────────────────────────────────────────────────────────
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _currentPosition = MutableStateFlow<LatLng?>(null)
    val currentPosition = _currentPosition.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                val newLatLng = LatLng.from(loc.latitude, loc.longitude)
                _currentPosition.value = newLatLng

                // 첫 위치 수신 시 주변 위험구역을 서버에서 로드
                if (!dangerZonesLoaded) {
                    dangerZonesLoaded = true
                    loadDangerZones(loc.latitude, loc.longitude)
                }

                // 산책 중이고 일시정지가 아닐 때만 GPS 포인트 기록
                if (_state.value.isWalking && !_state.value.isPaused) {
                    val prev = _routePoints.value.lastOrNull()
                    if (prev != null) {
                        val dist = haversineMeters(prev.latitude, prev.longitude, loc.latitude, loc.longitude)
                        _state.update { it.copy(distanceMeters = it.distanceMeters + dist) }
                    }
                    _routePoints.value = _routePoints.value + newLatLng
                    pendingPoints.add(
                        LocationBatchRequest.LocationPoint(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun startLocationTracking() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            // setMinUpdateDistanceMeters 제거: 정지 상태에서도 GPS 업데이트 허용
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

    // ── 카메라 사진 자동 감지 ───────────────────────────────────────────────────

    private fun startPhotoObserver() {
        // 미디어 읽기 권한 확인
        val mediaPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, mediaPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            android.util.Log.w("WalkVM", "미디어 읽기 권한 없음 — 사진 자동 감지 비활성화")
            return
        }

        walkStartTimestamp = System.currentTimeMillis() / 1000
        uploadedPhotoIds.clear()

        photoObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                val walkId = currentWalkId ?: return
                viewModelScope.launch {
                    checkAndUploadNewPhotos(walkId)
                }
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            photoObserver!!
        )
    }

    /** 권한 획득 후 산책 중이면 observer 재시작 */
    fun retryPhotoObserverIfWalking() {
        if (_state.value.isWalking && photoObserver == null) {
            startPhotoObserver()
        }
    }

    private fun stopPhotoObserver() {
        photoObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        photoObserver = null
        uploadedPhotoIds.clear()
    }

    private fun checkAndUploadNewPhotos(walkId: Long) {
        viewModelScope.launch {
            try {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.MIME_TYPE,
                )
                val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
                val selectionArgs = arrayOf(walkStartTimestamp.toString())
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, selectionArgs, sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val photoId = cursor.getLong(idCol)
                        if (photoId in uploadedPhotoIds) continue
                        uploadedPhotoIds.add(photoId)

                        val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                        val contentUri = android.content.ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoId
                        )

                        val stream = context.contentResolver.openInputStream(contentUri) ?: continue
                        val bytes = stream.readBytes()
                        stream.close()
                        val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData(
                            "files", "walk_photo_${photoId}.jpg", requestBody
                        )
                        walkRepository.uploadPhotos(walkId, listOf(part))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("WalkVM", "사진 자동 업로드 실패: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(sensorListener)
        stopLocationTracking()
        stopPhotoObserver()
    }

    // ── 산책 경로 ──────────────────────────────────────────────────────────────

    // 자유 산책 (첫 번째 고정 옵션)
    private val freeWalkRoute = WalkRoute(
        title = "자유 산책",
        subtitle = "자유롭게 산책해요"
    )

    /**
     * 전체 경로 목록 반환 (자유 산책 + 추천 경로들)
     * UI에서 사용
     */
    fun getDisplayRoutes(): List<WalkRoute> {
        val recommended = _state.value.recommendedRoutes.map { route ->
            WalkRoute(
                title = route.name,
                subtitle = route.getSubtitle(),
                distanceKm = route.distanceKm(),
                durationMin = route.estimatedMinutes
            )
        }
        return listOf(freeWalkRoute) + recommended
    }

    /**
     * 현재 선택된 추천 경로 반환 (자유 산책이면 null)
     */
    fun getSelectedRecommendedRoute(): RecommendedRoute? {
        val index = _state.value.selectedRouteIndex
        if (index == 0) return null  // 자유 산책
        val recommendedIndex = index - 1
        return _state.value.recommendedRoutes.getOrNull(recommendedIndex)
    }

    fun selectRoute(index: Int) {
        _state.update { it.copy(selectedRouteIndex = index) }
    }

    /** 추천 경로 표시 토글 */
    fun toggleRecommendedRouteVisibility() {
        _state.update { it.copy(showRecommendedRoute = !it.showRecommendedRoute) }
    }

    /**
     * 현재 위치 기반 추천 경로 로드
     * - 초기 로드 또는 위치 변경 시 호출
     * - 한 번 로드 후 캐시하여 재사용
     */
    fun loadRecommendedRoutes(latitude: Double, longitude: Double) {
        // 이미 로드했거나 로딩 중이면 skip
        if (_state.value.recommendedRoutes.isNotEmpty() || _state.value.isRoutesLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isRoutesLoading = true, routesError = null) }

            getRecommendedRoutesUseCase(latitude, longitude)
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            recommendedRoutes = response.routes,
                            fallbackLevel = response.fallbackLevel,
                            fallbackMessage = response.fallbackMessage,
                            isRoutesLoading = false,
                            routesError = null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isRoutesLoading = false,
                            routesError = e.message
                        )
                    }
                }
        }
    }

    /**
     * 추천 경로 강제 새로고침
     */
    fun refreshRecommendedRoutes(latitude: Double, longitude: Double) {
        _state.update { it.copy(recommendedRoutes = emptyList()) }
        loadRecommendedRoutes(latitude, longitude)
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

        // 주변 강아지 필터 ON/OFF → 폴링 제어
        if (WalkFilterType.NEARBY_DOG in pending && _state.value.isWalking) {
            startNearbyDogsPolling()
        } else {
            stopNearbyDogsPolling()
        }
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

    // ── 장소 상세 선택 ─────────────────────────────────────────────────────────

    /** 마커 클릭 시 선택된 장소 설정 후 상세 API 호출로 description 추가 */
    fun selectPlace(place: Place) {
        _state.update { it.copy(selectedPlace = place) }
        viewModelScope.launch {
            getPlaceDetailUseCase(place.id)
                .onSuccess { detail ->
                    // 현재 선택된 장소가 아직 같은 장소일 때만 업데이트
                    if (_state.value.selectedPlace?.id == detail.id) {
                        _state.update { it.copy(selectedPlace = detail) }
                    }
                }
        }
    }

    /** 장소 상세 바텀시트 닫기 */
    fun dismissPlaceDetail() {
        _state.update { it.copy(selectedPlace = null) }
    }

    // ── 산책 ID 관리 ───────────────────────────────────────────────────────────

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
        startPhotoObserver()

        // 백그라운드 서버 요청 — CompletableDeferred로 endWalk에서 대기 가능
        val deferred = CompletableDeferred<Long?>()
        walkIdDeferred = deferred
        viewModelScope.launch {
            startFreeWalkUseCase()
                .onSuccess { walkId ->

                    currentWalkId = walkId
                    _state.update { it.copy(currentWalkId = walkId) }
                    deferred.complete(walkId)
                    startBatchSending(walkId)
                    // NEARBY_DOG 필터가 이미 활성화된 경우 폴링 시작
                    if (WalkFilterType.NEARBY_DOG in _state.value.activeFilters) {
                        startNearbyDogsPolling()
                    }
                }
                .onFailure { e ->

                    deferred.complete(null)
                    _state.update { it.copy(walkError = "산책 시작 실패: ${e.message}") }
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
        stopNearbyDogsPolling()
        stopPhotoObserver()

        val summarySeconds = _state.value.elapsedSeconds
        val summaryDistance = _state.value.distanceMeters
        val summaryRoute = getDisplayRoutes().getOrNull(_state.value.selectedRouteIndex)?.title ?: "자유 산책"

        viewModelScope.launch {
            // currentWalkId가 아직 null이면 산책 시작 API 응답을 대기
            var walkId = currentWalkId
            if (walkId == null) {
                walkId = walkIdDeferred?.await()
            }

            if (walkId == null) {
                batchSendJob?.cancel()
                pendingPoints.clear()
                _state.update {
                    it.copy(
                        isWalking = false,
                        isPaused = false,
                        elapsedSeconds = 0,
                        distanceMeters = 0.0,
                        walkError = "산책 시작에 실패하여 기록이 저장되지 않았습니다.",
                        currentWalkId = null,
                        nearbyDogs = emptyList(),
                        isWalkSummaryVisible = true,
                        summaryElapsedSeconds = summarySeconds,
                        summaryDistanceMeters = summaryDistance,
                        summaryRouteName = summaryRoute,
                        summaryRating = 0,
                    )
                }
                return@launch
            }

            batchSendJob?.cancel()
            val remaining = pendingPoints.toList()
            pendingPoints.clear()
            if (remaining.size >= 2) {
                saveLocationsUseCase(walkId, remaining)
            }

            val result = endWalkUseCase(walkId)
            currentWalkId = null
            walkIdDeferred = null
            val newBadges = result.getOrDefault(emptyList())

            _state.update {
                it.copy(
                    isWalking = false,
                    isPaused = false,
                    elapsedSeconds = 0,
                    distanceMeters = 0.0,
                    walkError = null,
                    currentWalkId = null,
                    nearbyDogs = emptyList(),
                    isWalkSummaryVisible = true,
                    summaryWalkId = walkId,
                    summaryElapsedSeconds = summarySeconds,
                    summaryDistanceMeters = summaryDistance,
                    summaryRouteName = summaryRoute,
                    summaryRating = 0,
                    newBadges = newBadges,
                )
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
                summaryWalkId = null,
                summaryElapsedSeconds = 0,
                summaryDistanceMeters = 0.0,
                summaryRouteName = "",
                summaryRating = 0,
            )
        }
    }

    /** 배지 획득 알림 닫기 */
    fun dismissNewBadges() {
        _state.update { it.copy(newBadges = emptyList()) }
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
     */
    private fun startBatchSending(walkId: Long) {
        batchSendJob?.cancel()
        batchSendJob = viewModelScope.launch {
            while (true) {
                delay(10_000L)
                val batch = pendingPoints.toList()
                if (batch.size >= 2) {
                    pendingPoints.clear()
                    saveLocationsUseCase(walkId, batch)
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

    // ── 소셜 산책 — 주변 강아지 ───────────────────────────────────────────────

    /** 주변 강아지 + 제안 폴링 */
    private fun loadNearbyDogs() {
        val walkId = currentWalkId ?: return
        val pos = _currentPosition.value ?: return
        viewModelScope.launch {
            walkRepository.fetchNearbyDogsResponse(
                lat = pos.latitude,
                lon = pos.longitude,
                myWalkRecordId = walkId,
            ).onSuccess { response ->
                android.util.Log.d("WalkVM", "nearbyDogs 조회 성공: ${response.nearbyDogs.size}마리")
                _state.update {
                    it.copy(
                        nearbyDogs = response.nearbyDogs,
                        pendingProposals = response.pendingProposals,
                        acceptedProposals = response.acceptedProposals,
                    )
                }

                // 비선호 강아지가 주변에 있으면 경고 (세션 내 강아지당 1회)
                val shownIds = _state.value.shownWarningDogIds
                val dislikedDog = response.nearbyDogs
                    .firstOrNull { it.feedback == "싫어요" && it.dogId !in shownIds }
                if (dislikedDog != null) {
                    _state.update {
                        it.copy(
                            warningDog = dislikedDog,
                            shownWarningDogIds = it.shownWarningDogIds + dislikedDog.dogId,
                        )
                    }
                }
            }.onFailure { e ->
                android.util.Log.e("WalkVM", "nearbyDogs 조회 실패: ${e.message}", e)
            }
        }
    }

    /** 비선호 강아지 경고 다이얼로그 닫기 */
    fun dismissWarning() {
        _state.update { it.copy(warningDog = null) }
    }

    /** 5초 간격 폴링 시작 */
    private fun startNearbyDogsPolling() {
        nearbyDogsJob?.cancel()
        nearbyDogsJob = viewModelScope.launch {
            while (true) {
                loadNearbyDogs()
                delay(5_000L)
            }
        }
    }

    /** 폴링 중단 */
    private fun stopNearbyDogsPolling() {
        nearbyDogsJob?.cancel()
        nearbyDogsJob = null
    }

    // ── 강아지 공개 프로필 팝업 ────────────────────────────────────────────────

    /** 마커 클릭 → 강아지 선택 후 공개 프로필 로드 */
    fun selectNearbyDog(dog: NearbyDogResponse) {
        _state.update { it.copy(selectedNearbyDog = dog, dogPublicProfile = null, isDogProfileLoading = true) }
        loadDogPublicProfile(dog.dogId)
    }

    /** 팝업 닫기 */
    fun dismissDogProfile() {
        _state.update { it.copy(selectedNearbyDog = null, dogPublicProfile = null, isDogProfileLoading = false) }
    }

    /** 궁합 피드백 저장 — 좋아요 / 보통 / 싫어요 */
    fun updateFeedback(targetDogId: Long, feedback: String) {
        val walkId = currentWalkId ?: return
        viewModelScope.launch {
            walkRepository.updateFeedback(
                FeedbackRequest(
                    targetDogId = targetDogId,
                    myWalkRecordId = walkId,
                    feedback = feedback,
                )
            ).onFailure { e ->
                android.util.Log.e("WalkVM", "피드백 저장 실패: ${e.message}", e)
            }
        }
    }

    private fun loadDogPublicProfile(dogId: Long) {
        viewModelScope.launch {
            dogRepository.fetchPublicDogProfile(dogId)
                .onSuccess { profile ->
                    _state.update { it.copy(dogPublicProfile = profile, isDogProfileLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isDogProfileLoading = false) }
                }
        }
    }

    // ── 함께 산책 제안 ─────────────────────────────────────────────────────────

    /** 팝업에서 "함께 산책 제안" 버튼 클릭 */
    fun sendProposal(toWalkRecordId: Long, toDogId: Long) {
        val fromWalkRecordId = currentWalkId ?: return
        _state.update { it.copy(isSendingProposal = true) }
        viewModelScope.launch {
            walkRepository.sendProposal(fromWalkRecordId, toWalkRecordId)
                .onSuccess {
                    _state.update { it.copy(isSendingProposal = false, proposalSentDogId = toDogId) }
                }
                .onFailure {
                    _state.update { it.copy(isSendingProposal = false) }
                }
        }
    }

    /** 받은 제안 수락 */
    fun acceptProposal(proposal: PendingProposalInfo) {
        val myWalkRecordId = currentWalkId ?: return
        _state.update { it.copy(
            pendingProposals = it.pendingProposals.filter { p -> p.proposalId != proposal.proposalId }
        ) }
        viewModelScope.launch {
            walkRepository.respondToProposal(proposal.proposalId, "ACCEPT", myWalkRecordId)
        }
    }

    /** 받은 제안 거절 */
    fun rejectProposal(proposal: PendingProposalInfo) {
        val myWalkRecordId = currentWalkId ?: return
        _state.update { it.copy(
            pendingProposals = it.pendingProposals.filter { p -> p.proposalId != proposal.proposalId }
        ) }
        viewModelScope.launch {
            walkRepository.respondToProposal(proposal.proposalId, "REJECT", myWalkRecordId)
        }
    }

    /** 수락 알림 확인 (dismissed) */
    fun dismissAcceptedProposal(proposalId: String) {
        _state.update { it.copy(
            acceptedProposals = it.acceptedProposals.filter { a -> a.proposalId != proposalId }
        ) }
    }

    // ── 위험 구역 신고 ─────────────────────────────────────────────────────────

    /** 서버에서 주변 위험 구역을 불러와 지도에 표시 */
    fun loadDangerZones(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            getDangerZonesUseCase(latitude, longitude)
                .onSuccess { zones ->
                    // 기존 세션 신고분과 서버 데이터 병합 (id 기준 중복 제거)
                    _state.update { current ->
                        val existingIds = current.dangerZones.map { it.id }.toSet()
                        val newZones = zones.filter { it.id !in existingIds }
                        current.copy(dangerZones = current.dangerZones + newZones)
                    }
                }
                .onFailure { e ->
                    android.util.Log.w("WalkViewModel", "위험구역 로드 실패: ${e.message}")
                }
        }
    }

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
                val result = reportDangerZoneUseCase(
                    walkId = currentWalkId,
                    location = location,
                    reason = reason,
                    customReason = customReason
                ).getOrThrow()

                _state.update {
                    it.copy(
                        dangerZones = it.dangerZones + result.dangerZone,
                        isSelectingDangerZone = false,
                        selectedLocation = null,
                        isDangerReportDialogOpen = false,
                        selectedDangerReason = null,
                        customDangerReason = "",
                        isLoading = false,
                        error = null,
                        newBadges = it.newBadges + result.newBadges
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
