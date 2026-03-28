package com.frontend.ui.screen.walk

import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
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
import com.frontend.data.remote.StompChatClient
import com.frontend.data.repository.DogRepository
import com.frontend.data.repository.HomeRepository
import com.frontend.data.repository.WalkRepository
import com.frontend.domain.model.ChatBannerNotification
import com.frontend.domain.model.FeedbackRequest
import com.frontend.domain.model.HomeWeatherInfo
import com.frontend.domain.model.NearbyDogResponse
import com.frontend.domain.model.PendingProposalInfo
import com.frontend.domain.model.RejectedProposalInfo
import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.LocationBatchRequest
import com.frontend.domain.model.Place
import com.frontend.domain.model.RecommendedRoute
import com.frontend.domain.model.StartWalkRequest
import com.frontend.domain.model.WalkRoute
import com.frontend.domain.model.NearbyDangerZone
import com.frontend.domain.usecase.EndWalkUseCase
import com.frontend.domain.usecase.GetDangerZonesUseCase
import com.frontend.domain.usecase.GetFootprintPlacesUseCase
import com.frontend.domain.usecase.GetMyRiskZonesUseCase
import com.frontend.domain.usecase.GetNearbyMyRiskZonesUseCase
import com.frontend.domain.usecase.GetPlaceDetailUseCase
import com.frontend.domain.usecase.GetPlacesUseCase
import com.frontend.domain.usecase.GetRecommendedRoutesUseCase
import com.frontend.domain.usecase.ReportDangerZoneUseCase
import com.frontend.domain.usecase.SaveLocationsUseCase
import com.frontend.domain.usecase.SendStampUseCase
import com.frontend.domain.usecase.StampPlaceUseCase
import com.frontend.domain.usecase.StartFreeWalkUseCase
import com.frontend.notification.FootprintAlertManager
import com.frontend.notification.NearbyDogAlertManager
import com.frontend.notification.RiskZoneAlertManager
import com.frontend.wearable.WearableAction
import com.frontend.wearable.WearableActionBus
import com.frontend.wearable.WearableManager
import org.json.JSONObject
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
import kotlinx.coroutines.withTimeoutOrNull
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
    private val getMyRiskZonesUseCase: GetMyRiskZonesUseCase,
    private val getNearbyMyRiskZonesUseCase: GetNearbyMyRiskZonesUseCase,
    private val getPlacesUseCase: GetPlacesUseCase,
    private val getFootprintPlacesUseCase: GetFootprintPlacesUseCase,
    private val walkRepository: WalkRepository,
    private val homeRepository: HomeRepository,
    private val tokenDataStore: TokenDataStore,
    private val dogRepository: DogRepository,
    private val getPlaceDetailUseCase: GetPlaceDetailUseCase,
    private val getRecommendedRoutesUseCase: GetRecommendedRoutesUseCase,
    private val startFreeWalkUseCase: StartFreeWalkUseCase,
    private val saveLocationsUseCase: SaveLocationsUseCase,
    private val endWalkUseCase: EndWalkUseCase,
    private val nearbyDogAlertManager: NearbyDogAlertManager,
    private val footprintAlertManager: FootprintAlertManager,
    private val riskZoneAlertManager: RiskZoneAlertManager,
    private val stampPlaceUseCase: StampPlaceUseCase,
    private val stompChatClient: StompChatClient,
    private val wearableManager: WearableManager,
) : ViewModel() {

    private data class RouteWeatherPayload(
        val weatherCondition: String?,
        val temperature: Double?,
    )

    // ── 비선호 강아지 알림 상수 ─────────────────────────────────────────────────
    companion object {
        private const val ALERT_ENTER_RADIUS_M = 50.0   // 알림 발생 반경
        private const val ALERT_EXIT_RADIUS_M = 70.0    // 반경 이탈 판정 거리
        private const val ALERT_COOLDOWN_MS = 2 * 60 * 1000L  // 2분 쿨다운
        private const val FOOTPRINT_ALERT_RADIUS_M = 20.0     // 발자국 알림 반경

        // 위험장소 알림 상수
        private const val RISK_ZONE_ENTER_RADIUS_M = 100.0    // 알림 발생 반경
        private const val RISK_ZONE_EXIT_RADIUS_M = 150.0     // 반경 이탈 판정 거리
        private const val RISK_ZONE_COOLDOWN_MS = 3 * 60 * 1000L  // 3분 쿨다운
        private const val RISK_ZONE_POLLING_INTERVAL_MS = 15_000L // 15초 폴링
        private const val RISK_ZONE_QUERY_RADIUS_M = 500.0    // nearby 조회 반경
        private const val DANGER_ZONE_ALERT_RADIUS_M = 100.0  // 워치 위험구역 알림 반경
        private const val DANGER_ZONE_COOLDOWN_MS = 5 * 60 * 1000L // 워치 알림 5분 쿨다운
    }

    private val _state = MutableStateFlow(WalkState())
    val state = _state.asStateFlow()
    private var cachedWeatherPayload: RouteWeatherPayload? = null

    // ── 주변 강아지 폴링 Job ───────────────────────────────────────────────────
    private var nearbyDogsJob: Job? = null

    // ── 위험장소 근접 알림 폴링 Job ───────────────────────────────────────────────
    private var riskZonePollingJob: Job? = null

    // ── 현재 산책 ID (산책 시작 후 서버에서 발급) ──────────────────────────────
    private var currentWalkId: Long? = null
    // 산책 시작 API 응답을 기다리기 위한 Deferred (endWalk에서 대기 가능)
    private var walkIdDeferred: CompletableDeferred<Long?>? = null
    // 산책 세션 ID: startFreeWalk()마다 증가 → endWalk() 코루틴이 새 산책 상태를 덮어쓰는 race condition 방지
    private var walkSessionId = 0

    // ── 위험구역 최초 로드 여부 (위치 수신 후 1회만 로드) ───────────────────────
    private var dangerZonesLoaded = false

    // ── 위험구역 워치 알림 쿨다운 (zoneId → 마지막 알림 시각) ─────────────────
    private val dangerZoneAlertTimes = mutableMapOf<Long, Long>()

    // ── 발자국 체크용 주변 장소 최초 로드 여부 (산책 시작 후 1회만 로드) ───────
    private var walkPlacesLoaded = false

    // ── 산책 경로 포인트 (지도 경로 표시용) ────────────────────────────────────
    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints = _routePoints.asStateFlow()

    // ── GPS 배치 전송 버퍼 ──────────────────────────────────────────────────────
    private val pendingPoints = mutableListOf<LocationBatchRequest.LocationPoint>()

    // ── 타이머 / 배치 전송 Job ──────────────────────────────────────────────────
    private var timerJob: Job? = null
    private var batchSendJob: Job? = null

    // ── 발자국 도장 후보 장소 (산책 중 근접 감지용) ────────────────────────────
    private var stampCandidates: List<Place> = emptyList()
    private var stampCandidatesCenter: LatLng? = null

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
        // 내 위험장소 영구 목록 로드 (앱/화면 진입 시)
        loadPersistedDangerZones()

        // 워치→폰 액션 수신
        // ViewModel 생성 전에 도착한 액션 처리: SharedFlow는 replay=0이라 새 collector가 과거 이벤트를 받지 못함
        // WearableActionBus의 StateFlow(pendingStartWalk/pendingCourseIndex)로 보완
        if (WearableActionBus.pendingStartWalk.value) {
            WearableActionBus.consumePendingStartWalk()
            startFreeWalk()
        }
        WearableActionBus.pendingCourseIndex.value?.let { courseIndex ->
            WearableActionBus.consumePendingCourseIndex()
            selectRoute(courseIndex)
            if (!_state.value.isWalking) startFreeWalk()
        }

        viewModelScope.launch {
            WearableActionBus.actions.collect { action ->
                when (action) {
                    is WearableAction.StartWalk -> {
                        WearableActionBus.consumePendingStartWalk()
                        if (!_state.value.isWalking) startFreeWalk()
                    }
                    is WearableAction.SelectCourse -> {
                        // isWalking 중일 때는 무시 (이전 endWalk 코루틴이 아직 실행 중인 경우 대비)
                        if (!_state.value.isWalking) {
                            WearableActionBus.consumePendingCourseIndex()
                            selectRoute(action.courseIndex)
                            startFreeWalk()
                        }
                    }
                    is WearableAction.EndWalk -> endWalk()
                    is WearableAction.PauseWalk -> pauseWalk()
                    is WearableAction.ResumeWalk -> resumeWalk()
                    is WearableAction.ProposalAccept -> {
                        walkRepository.respondToProposal(action.proposalId, "ACCEPT", action.myWalkRecordId)
                            .onSuccess { chatRoomId ->
                                android.util.Log.d("WalkVM", "워치 제안 수락 처리 완료: chatRoomId=$chatRoomId")
                            }
                            .onFailure { e ->
                                android.util.Log.e("WalkVM", "워치 제안 수락 실패: ${e.message}")
                            }
                    }
                    is WearableAction.ProposalReject -> {
                        walkRepository.respondToProposal(action.proposalId, "REJECT", action.myWalkRecordId)
                            .onFailure { e ->
                                android.util.Log.e("WalkVM", "워치 제안 거절 실패: ${e.message}")
                            }
                    }
                    is WearableAction.Stamp -> {
                        stampPlaceUseCase(action.walkId, action.dogId, action.placeId)
                            .onFailure { e ->
                                android.util.Log.e("WalkVM", "워치 발자국 도장 실패: ${e.message}")
                            }
                    }
                }
            }
        }
    }

    /**
     * 내 위험장소 영구 목록 로드 (mine API)
     * - 앱/화면 재진입 시 개인 위험장소 복원
     */
    fun loadPersistedDangerZones() {
        viewModelScope.launch {
            getMyRiskZonesUseCase()
                .onSuccess { zones ->
                    _state.update { it.copy(persistedDangerZones = zones) }
                    android.util.Log.d("WalkVM", "내 위험장소 로드 성공: ${zones.size}개")
                }
                .onFailure { e ->
                    android.util.Log.w("WalkVM", "내 위험장소 로드 실패: ${e.message}")
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
                if (loc.accuracy > 20f) return@let
                val newLatLng = LatLng.from(loc.latitude, loc.longitude)
                _currentPosition.value = newLatLng

                // 첫 위치 수신 시 주변 위험구역을 서버에서 로드
                if (!dangerZonesLoaded) {
                    dangerZonesLoaded = true
                    loadDangerZones(loc.latitude, loc.longitude)
                }

                // 위험구역 근접 시 워치 알림
                if (_state.value.isWalking && !_state.value.isPaused) {
                    checkDangerZoneProximity(loc.latitude, loc.longitude)
                }

                // 산책 중이고 일시정지가 아닐 때만 GPS 포인트 기록 + 근접 장소 감지
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

                    // 발자국 체크용 장소 최초 로드
                    if (!walkPlacesLoaded) {
                        walkPlacesLoaded = true
                        loadWalkPlaces(loc.latitude, loc.longitude)
                    }
                    // 20m 이내 장소 진입 감지
                    checkNearbyPlacesForFootprint(newLatLng)
                    // 발자국 도장 후보 장소 근접 감지
                    checkAndUpdateStampPrompt(loc.latitude, loc.longitude)
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
        // 미디어 전체 접근 권한 확인
        // 자동 감지는 MediaStore 쿼리로 새 사진을 찾으므로 전체 접근 필수
        // (Android 14+ "사진 선택" 부분 접근으로는 새 카메라 사진 감지 불가)
        val hasFullAccess = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_MEDIA_IMAGES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (!hasFullAccess) {
            android.util.Log.w("WalkVM", "미디어 전체 접근 권한 없음 — 사진 자동 감지 비활성화 (산책 후 수동 업로드 가능)")
            return
        }

        walkStartTimestamp = System.currentTimeMillis()
        uploadedPhotoIds.clear()

        photoObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                val walkId = currentWalkId ?: return
                viewModelScope.launch {
                    // walkId가 아직 없으면 서버 응답을 기다림
                    val walkId = currentWalkId ?: walkIdDeferred?.await() ?: return@launch
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
        // uploadedPhotoIds는 여기서 지우지 않음
        // endWalk()에서 마지막 스캔이 완료되기 전에 지워지면 이미 업로드된 사진이 재업로드됨
        // 다음 산책 시작 시 startPhotoObserver()에서 clear됨
    }

    private val uploadMutex = kotlinx.coroutines.sync.Mutex()
    private var pendingUploadJob: kotlinx.coroutines.Job? = null

    private fun checkAndUploadNewPhotos(walkId: Long) {
        // debounce: ContentObserver가 사진 하나에 여러 번 fire → 500ms 대기 후 한 번만 처리
        // 단, debounce만 취소하고 이미 진행 중인 업로드는 취소하지 않음
        pendingUploadJob?.cancel()
        pendingUploadJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            // NonCancellable: 업로드 시작 후에는 취소 방지 → uploadedPhotoIds 누락 방지
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                uploadMutex.lock()
                try {
                    val projection = arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.MIME_TYPE,
                    )
                    val walkStartSeconds = walkStartTimestamp / 1000
                    val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
                    val selectionArgs = arrayOf(walkStartSeconds.toString())
                    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection, selection, selectionArgs, sortOrder
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                        // 먼저 업로드할 사진 목록을 수집 (cursor 순회 중 네트워크 호출 방지)
                        data class PendingPhoto(val id: Long, val mime: String)
                        val pendingPhotos = mutableListOf<PendingPhoto>()

                        while (cursor.moveToNext()) {
                            val photoId = cursor.getLong(idCol)
                            if (photoId in uploadedPhotoIds) continue
                            // 업로드 시작 전에 미리 등록 → 중복 방지
                            uploadedPhotoIds.add(photoId)
                            val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                            pendingPhotos.add(PendingPhoto(photoId, mime))
                        }

                        for (photo in pendingPhotos) {
                            val ext = when {
                                photo.mime.contains("png") -> "png"
                                photo.mime.contains("webp") -> "webp"
                                photo.mime.contains("heic") || photo.mime.contains("heif") -> "heic"
                                else -> "jpg"
                            }
                            val contentUri = android.content.ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photo.id
                            )

                            val bytes = compressImage(contentUri)
                            if (bytes == null) {
                                uploadedPhotoIds.remove(photo.id) // 압축 실패 시 재시도 허용
                                continue
                            }
                            val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData(
                                "files", "walk_photo_${photo.id}.${ext}", requestBody
                            )
                            val result = walkRepository.uploadPhotos(walkId, listOf(part))
                            if (!result.isSuccess) {
                                uploadedPhotoIds.remove(photo.id) // 업로드 실패 시 재시도 허용
                                android.util.Log.w("WalkVM", "사진 업로드 실패 (photoId=${photo.id}): ${result.exceptionOrNull()?.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("WalkVM", "사진 자동 업로드 실패: ${e.message}")
                } finally {
                    uploadMutex.unlock()
                }
            }
        }
    }

    /** 사진을 최대 1920px, JPEG 80% 품질로 압축 + EXIF 회전 보정 (413 방지) */
    private fun compressImage(uri: Uri, maxDimension: Int = 1920, quality: Int = 80): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            // EXIF orientation 읽어서 회전 보정
            val rotated = context.contentResolver.openInputStream(uri)?.use { exifStream ->
                val exif = ExifInterface(exifStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.preScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.preScale(-1f, 1f) }
                    else -> null
                }?.let {
                    Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                }
            } ?: original
            if (rotated !== original) original.recycle()

            val ratio = minOf(maxDimension.toFloat() / rotated.width, maxDimension.toFloat() / rotated.height, 1f)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(rotated, (rotated.width * ratio).toInt(), (rotated.height * ratio).toInt(), true)
            } else rotated

            val output = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            if (scaled !== rotated) scaled.recycle()
            rotated.recycle()
            output.toByteArray()
        } catch (e: Exception) {
            android.util.Log.w("WalkVM", "사진 압축 실패: ${e.message}")
            null
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
                durationMin = route.getDisplayDurationMinutes()
            )
        }
        return listOf(freeWalkRoute) + recommended
    }

    // Temporary guard: replace obviously broken route times with a distance-based estimate.
    private fun RecommendedRoute.getDisplayDurationMinutes(): Int {
        val fallbackMinutes = kotlin.math.ceil(totalDistanceM / 67.0).toInt().coerceAtLeast(1)
        if (estimatedMinutes <= 0) return fallbackMinutes

        val isSuspiciouslyShort = fallbackMinutes >= 10 && estimatedMinutes * 2 < fallbackMinutes
        return if (isSuspiciouslyShort) fallbackMinutes else estimatedMinutes
    }

    private suspend fun getCurrentWeatherPayload(): RouteWeatherPayload? {
        cachedWeatherPayload?.let { return it }
        val pos = _currentPosition.value ?: return null
        return fetchWeatherPayload(pos.latitude, pos.longitude)
    }

    private suspend fun fetchWeatherPayload(latitude: Double, longitude: Double): RouteWeatherPayload? {
        return homeRepository.getHomeData(latitude, longitude)
            .getOrNull()
            ?.weather
            ?.toRouteWeatherPayload()
            ?.also { cachedWeatherPayload = it }
    }

    private fun HomeWeatherInfo.toRouteWeatherPayload(): RouteWeatherPayload {
        val condition = when {
            weatherCode == "SNOWY" -> "SNOW"
            weatherCode == "RAINY" -> "RAIN"
            temperature >= 30 -> "HOT"
            temperature <= 0 -> "COLD"
            weatherCode == "CLOUDY" -> "CLOUDY"
            weatherCode == "SUNNY" -> "CLEAR"
            else -> null
        }

        return RouteWeatherPayload(
            weatherCondition = condition,
            temperature = temperature.toDouble()
        )
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
            val weatherPayload = fetchWeatherPayload(latitude, longitude)

            getRecommendedRoutesUseCase(
                latitude = latitude,
                longitude = longitude,
                weather = weatherPayload?.weatherCondition
            )
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
                    // 워치로 코스 목록 전송
                    val courses = mutableListOf(
                        WearableManager.CourseInfo(0, "FREE", "자유 산책", 0.0, 0)
                    )
                    response.routes.forEachIndexed { idx, route ->
                        courses.add(
                            WearableManager.CourseInfo(
                                index = idx + 1,
                                type = route.type,
                                name = route.name,
                                distanceKm = route.totalDistanceM / 1000.0,
                                durationMin = route.estimatedMinutes,
                            )
                        )
                    }
                    wearableManager.sendCourseData(courses)
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
     * 경로 목록을 비우면서 selectedRouteIndex도 0(자유 산책)으로 초기화
     */
    fun refreshRecommendedRoutes(latitude: Double, longitude: Double) {
        _state.update { it.copy(recommendedRoutes = emptyList(), selectedRouteIndex = 0) }
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
        val wasFootprintActive = WalkFilterType.FOOTPRINT in _state.value.activeFilters
        val isFootprintActive = WalkFilterType.FOOTPRINT in pending

        _state.update { it.copy(activeFilters = pending, showFilterSheet = false) }

        // 장소 필터가 꺼진 경우 → 장소 목록 초기화 (마커 제거 + 재조회는 Screen에서 처리)
        if (!isPlaceActive && wasPlaceActive) {
            _state.update { it.copy(places = emptyList()) }
        }
        // 장소 필터 ON 시 로드는 WalkScreen의 LaunchedEffect(activeFilters)에서 지도 중심으로 처리

        // 발자국 필터 ON → 도장 찍은 장소 로드 / OFF → 초기화
        if (isFootprintActive && !wasFootprintActive) {
            loadFootprintPlaces()
        } else if (!isFootprintActive && wasFootprintActive) {
            _state.update { it.copy(footprintPlaces = emptyList()) }
        }

        // 주변 강아지 필터는 마커 표시만 제어 (폴링은 산책 중 항상 실행 - 알림용)
        // 마커 표시/숨김은 WalkScreen에서 처리
    }

    /** 발자국 도장 찍은 장소 목록 로드 */
    fun loadFootprintPlaces() {
        viewModelScope.launch {
            val dogId = tokenDataStore.getDogId().first() ?: return@launch
            _state.update { it.copy(isFootprintPlacesLoading = true) }
            getFootprintPlacesUseCase(dogId)
                .onSuccess { places ->
                    _state.update { it.copy(footprintPlaces = places, isFootprintPlacesLoading = false) }
                }
                .onFailure {
                    _state.update { it.copy(isFootprintPlacesLoading = false) }
                }
        }
    }

    // ── 발자국 도장 근접 감지 ──────────────────────────────────────────────────

    /** 현재 위치와 후보 장소 간 근접 여부 확인, 50m 이내 장소가 있으면 stamp prompt 표시 */
    private fun checkAndUpdateStampPrompt(lat: Double, lon: Double) {
        val loadedAt = stampCandidatesCenter
        // 처음이거나 300m 이상 이동했으면 후보 재로드
        if (loadedAt == null ||
            haversineMeters(loadedAt.latitude, loadedAt.longitude, lat, lon) > 300.0
        ) {
            loadStampCandidates(lat, lon)
            return
        }
        val stampedIds = _state.value.stampedPlaceIds
        val nearbyPlace = stampCandidates.firstOrNull { place ->
            place.id !in stampedIds &&
                haversineMeters(lat, lon, place.latitude, place.longitude) <= 50.0
        }
        _state.update { it.copy(nearbyStampablePlace = nearbyPlace) }
    }

    /** 도장 후보 장소 목록 로드 (반경 500m) */
    private fun loadStampCandidates(lat: Double, lon: Double) {
        stampCandidatesCenter = LatLng.from(lat, lon)
        viewModelScope.launch {
            getPlacesUseCase(lat, lon, radius = 500.0, limit = 30)
                .onSuccess { places ->
                    stampCandidates = places
                    checkAndUpdateStampPrompt(lat, lon)
                }
        }
    }

    /** 발자국 도장 프롬프트 닫기 (이동 후 다시 나타날 수 있음) */
    fun dismissStampPrompt() {
        _state.update { it.copy(nearbyStampablePlace = null) }
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
     * 산책 시작 실패 시 상태를 완전히 원복
     */
    private fun resetWalkStateOnFailure(errorMessage: String) {
        android.util.Log.e("WalkVM", "산책 시작 실패: $errorMessage")
        timerJob?.cancel()
        timerJob = null
        stopPhotoObserver()
        walkIdDeferred?.complete(null)
        walkIdDeferred = null
        currentWalkId = null
        _routePoints.value = emptyList()
        pendingPoints.clear()
        _state.update {
            it.copy(
                isWalking = false,
                isPaused = false,
                elapsedSeconds = 0,
                distanceMeters = 0.0,
                currentWalkId = null,
                walkError = errorMessage,
            )
        }
    }

    /**
     * 산책 시작 (자유 산책 / 추천 경로 산책 통합)
     *
     * - selectedRouteIndex == 0: 자유 산책 (selectedType = null)
     * - selectedRouteIndex > 0: 추천 경로 산책 (selectedType, placeIds, recommendedPath 포함)
     *
     * 버튼 클릭 즉시 UI 전환 후 백그라운드에서 서버 요청
     * 실패 시 상태를 완전히 원복하여 산책 중 상태가 남지 않도록 보장
     */
    fun startFreeWalk() {
        walkSessionId++  // 새 산책 세션 시작 — 진행 중인 endWalk 코루틴이 이 산책 상태를 덮어쓰지 못하도록 방어
        // 즉시 UI 전환
        walkPlacesLoaded = false
        _state.update {
            it.copy(
                isWalking = true,
                isPaused = false,
                elapsedSeconds = 0,
                distanceMeters = 0.0,
                walkError = null,
                walkPlaces = emptyList(),
                footprintAlertStates = emptyMap(),
                footprintAlertPlace = null,
                footprintStamped = false,
            )
        }
        _routePoints.value = emptyList()
        pendingPoints.clear()
        startWalkTimer()
        startPhotoObserver()
        // 워치에 산책 시작 전송
        viewModelScope.launch {
            wearableManager.sendWalkStats(0, 0.0, 0, isWalking = true, isPaused = false)
        }

        // 백그라운드 서버 요청 — CompletableDeferred로 endWalk에서 대기 가능
        val deferred = CompletableDeferred<Long?>()
        walkIdDeferred = deferred
        viewModelScope.launch {
            // dogId 조회
            val dogId = tokenDataStore.getDogId().first()
            if (dogId == null) {
                resetWalkStateOnFailure("강아지 정보가 없습니다")
                return@launch
            }

            // 선택된 경로에 따라 요청 바디 구성
            val request = buildStartWalkRequest(dogId)
            if (request == null) {
                // 추천 경로 선택했으나 경로 데이터가 없는 경우
                resetWalkStateOnFailure("선택한 추천 경로를 찾을 수 없습니다. 경로를 다시 선택해주세요.")
                return@launch
            }

            startFreeWalkUseCase(request)
                .onSuccess { walkId ->
                    currentWalkId = walkId
                    _state.update { it.copy(currentWalkId = walkId) }
                    deferred.complete(walkId)
                    startBatchSending(walkId)
                    // 산책 시작 시 항상 nearby dogs 폴링 시작 (알림은 필터와 무관하게 동작)
                    startNearbyDogsPolling()
                    // 산책 시작 시 위험장소 근접 알림 폴링 시작
                    startRiskZonePolling()
                }
                .onFailure { e ->
                    resetWalkStateOnFailure("산책 시작 실패: ${e.message}")
                }
        }
    }

    /**
     * 선택된 경로에 따라 StartWalkRequest 생성
     *
     * @return StartWalkRequest 또는 null (추천 경로 선택했으나 경로 데이터가 없는 경우)
     */
    private suspend fun buildStartWalkRequest(dogId: Long): StartWalkRequest? {
        val selectedIndex = _state.value.selectedRouteIndex
        val weatherPayload = getCurrentWeatherPayload()

        // 자유 산책 (index == 0)
        if (selectedIndex == 0) {
            return StartWalkRequest(
                dogId = dogId,
                weatherCondition = weatherPayload?.weatherCondition,
                temperature = weatherPayload?.temperature
            )
        }

        // 추천 경로 산책: 경로가 없으면 null 반환 (자유 산책 fallback 금지)
        val recommendedRoute = getSelectedRecommendedRoute()
            ?: return null

        // recommendedPath: actualPathPoints 우선, 없으면 polyline 사용
        // 각 좌표를 [latitude, longitude] 형태로 변환
        val pathPoints = recommendedRoute.getPathPoints()
        val recommendedPath = pathPoints.map { point ->
            listOf(point.latitude, point.longitude)
        }

        return StartWalkRequest(
            dogId = dogId,
            selectedType = recommendedRoute.type,
            selectedDistanceM = recommendedRoute.totalDistanceM,
            placeIds = recommendedRoute.places.map { it.id },
            weatherCondition = null,  // 현재 구조상 날씨 정보 미제공 (optional)
            temperature = null,       // 현재 구조상 기온 정보 미제공 (optional)
            recommendedPath = recommendedPath.ifEmpty { null }
        ).copy(
            weatherCondition = weatherPayload?.weatherCondition,
            temperature = weatherPayload?.temperature
        )
    }

    /**
     * W1-06: 산책 종료
     * - 타이머 정지
     * - 남은 GPS 포인트 서버 전송
     * - 요약 화면 표시 (routePoints는 요약 화면에서 지도 표시용으로 유지)
     * - 비선호 강아지 알림 상태 정리
     */
    fun endWalk() {
        val sessionId = walkSessionId  // 현재 세션 캡처 — 코루틴 완료 시 새 산책이 시작됐는지 검증용
        timerJob?.cancel()
        timerJob = null
        stopNearbyDogsPolling()
        stopRiskZonePolling()

        // 산책 종료 직전 마지막으로 새 사진 스캔 (ContentObserver 누락 대비)
        // stopPhotoObserver() 전에 호출해야 uploadedPhotoIds가 살아있어 중복 업로드 방지
        currentWalkId?.let { walkId ->
            viewModelScope.launch { checkAndUploadNewPhotos(walkId) }
        }
        stopPhotoObserver()

        // 워치에 산책 종료 전송
        viewModelScope.launch {
            wearableManager.sendWalkStats(0, 0.0, 0, isWalking = false, isPaused = false)
            wearableManager.clearWalkStats()
        }

        // 비선호 강아지 알림 정리
        nearbyDogAlertManager.cancelAllAlerts()
        footprintAlertManager.cancelAlert()
        // 위험장소 알림 정리
        riskZoneAlertManager.cancelAllAlerts()
        _state.update {
            it.copy(
                dogAlertStates = emptyMap(),
                warningDog = null,
                footprintAlertPlace = null,
                footprintStamped = false,
                walkPlaces = emptyList(),
                footprintAlertStates = emptyMap(),
                // 위험장소 알림 상태 초기화
                nearbyDangerZones = emptyList(),
                riskZoneAlertStates = emptyMap(),
                warningRiskZoneQueue = emptyList(),
            )
        }

        // 발자국 도장 프롬프트 및 후보 장소 초기화
        stampCandidates = emptyList()
        stampCandidatesCenter = null
        _state.update { it.copy(nearbyStampablePlace = null, stampedPlaceIds = emptySet()) }

        // summarySeconds/Distance 캡처 후 즉시 isWalking=false 전환:
        // - 워치가 새 코스를 빠르게 선택해도 endWalk 코루틴 완료를 기다리지 않고 새 산책 시작 가능
        // - 타이머가 멈춘 채 UI가 '산책 중' 상태로 유지되는 현상 제거
        val summarySeconds = _state.value.elapsedSeconds
        val summaryDistance = _state.value.distanceMeters
        val summaryRoute = getDisplayRoutes().getOrNull(_state.value.selectedRouteIndex)?.title ?: "자유 산책"

        _state.update {
            it.copy(
                isWalking = false,
                isPaused = false,
                elapsedSeconds = 0,
                distanceMeters = 0.0,
                nearbyDogs = emptyList(),
                currentWalkId = null,
            )
        }

        viewModelScope.launch {
            // 새 산책이 이미 시작됐으면 stale endWalk 코루틴 무시
            if (sessionId != walkSessionId) {
                android.util.Log.d("WalkVM", "Stale endWalk call ignored for session $sessionId, current session is $walkSessionId")
                return@launch
            }
            // currentWalkId가 아직 null이면 산책 시작 API 응답을 대기
            var walkId = currentWalkId
            if (walkId == null) {
                walkId = walkIdDeferred?.await()
            }

            if (walkId == null) {
                batchSendJob?.cancel()
                pendingPoints.clear()
                // 새 산책이 이미 시작됐으면 요약 화면을 띄우지 않음
                if (walkSessionId == sessionId) {
                    _state.update {
                        it.copy(
                            walkError = "산책 시작에 실패하여 기록이 저장되지 않았습니다.",
                            isWalkSummaryVisible = true,
                            summaryElapsedSeconds = summarySeconds,
                            summaryDistanceMeters = summaryDistance,
                            summaryRouteName = summaryRoute,
                            summaryRating = 0,
                        )
                    }
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
            // 새 산책이 시작되지 않은 경우에만 walkId/Deferred 초기화 (새 산책의 ID를 지우지 않도록)
            if (walkSessionId == sessionId) {
                currentWalkId = null
                walkIdDeferred = null
            }
            val newBadges = result.getOrDefault(emptyList())
            // 워치에 배지 획득 알림 전송
            newBadges.forEach { badge ->
                wearableManager.sendNotification("badge", JSONObject().apply {
                    put("badgeId", badge.badgeId)
                    put("badgeName", badge.badgeName)
                })
            }

            // 새 산책이 이미 시작됐으면 이전 산책의 요약 화면을 덮어쓰지 않음
            if (walkSessionId == sessionId) {
                _state.update {
                    it.copy(
                        walkError = null,
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
        viewModelScope.launch {
            val s = _state.value
            wearableManager.sendWalkStats(s.elapsedSeconds, s.distanceMeters, (s.distanceMeters * 0.06).toInt(), true, true)
        }
    }

    /** 산책 재개 */
    fun resumeWalk() {
        _state.update { it.copy(isPaused = false) }
        viewModelScope.launch {
            val s = _state.value
            wearableManager.sendWalkStats(s.elapsedSeconds, s.distanceMeters, (s.distanceMeters * 0.06).toInt(), true, false)
        }
    }

    /** 1초마다 elapsedSeconds 증가 (일시정지 중에는 멈춤) */
    private fun startWalkTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val s = _state.value
                if (s.isWalking && !s.isPaused) {
                    val newSeconds = s.elapsedSeconds + 1
                    _state.update { it.copy(elapsedSeconds = newSeconds) }
                    // 워치에 산책 통계 전송
                    wearableManager.sendWalkStats(
                        elapsedSeconds = newSeconds,
                        distanceMeters = s.distanceMeters,
                        calories = (s.distanceMeters * 0.06).toInt(),
                        isWalking = true,
                        isPaused = false,
                    )
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

    // ── 발자국 장소 근접 감지 ─────────────────────────────────────────────────

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

                // 워치에 새 산책 제안 전송 (state 업데이트 전에 old 비교)
                val oldProposalIds = _state.value.pendingProposals.map { it.proposalId }.toSet()
                response.pendingProposals
                    .filter { it.proposalId !in oldProposalIds }
                    .forEach { proposal ->
                        wearableManager.sendInteractiveMessage("proposal", JSONObject().apply {
                            put("proposalId", proposal.proposalId)
                            put("dogName", proposal.name)
                            put("breed", proposal.breed)
                            put("myWalkRecordId", walkId)
                        })
                    }

                _state.update {
                    it.copy(
                        nearbyDogs = response.nearbyDogs,
                        pendingProposals = response.pendingProposals,
                        acceptedProposals = it.acceptedProposals + response.acceptedProposals,
                        rejectedProposals = it.rejectedProposals + response.rejectedProposals,
                    )
                }

                // 비선호 강아지 알림 처리 (avoidAlertCandidate 기반)
                processAvoidAlerts(response.nearbyDogs)
            }.onFailure { e ->
                android.util.Log.e("WalkVM", "nearbyDogs 조회 실패: ${e.message}", e)
            }
        }
    }

    /**
     * 비선호 강아지 알림 로직
     * - avoidAlertCandidate == true && distanceM <= 50m → 알림 발생
     * - distanceM > 70m 또는 목록에서 사라지면 → 다시 알림 가능
     * - 같은 dogId에 대해 2분 쿨다운
     * - 반경 안에 머무는 동안 알림 반복하지 않음
     */
    private fun processAvoidAlerts(nearbyDogs: List<NearbyDogResponse>) {
        val now = System.currentTimeMillis()
        val currentStates = _state.value.dogAlertStates.toMutableMap()
        val nearbyDogIds = nearbyDogs.map { it.dogId }.toSet()

        // 1. 목록에서 사라진 강아지 → 상태 초기화 (다시 알림 가능)
        val removedDogIds = currentStates.keys - nearbyDogIds
        removedDogIds.forEach { dogId ->
            android.util.Log.d("WalkVM", "비선호 강아지 $dogId 목록에서 사라짐 → 상태 초기화")
            currentStates.remove(dogId)
        }

        // 2. avoidAlertCandidate == true인 강아지 처리
        var newWarningDog: NearbyDogResponse? = null

        for (dog in nearbyDogs) {
            if (!dog.avoidAlertCandidate) continue

            val dogId = dog.dogId
            val alertState = currentStates[dogId] ?: DogAlertState()
            val wasInsideRadius = alertState.isInsideAlertRadius
            val isNowInsideRadius = dog.distanceM <= ALERT_ENTER_RADIUS_M
            val hasExitedRadius = dog.distanceM > ALERT_EXIT_RADIUS_M

            // 반경 이탈 시 → 다시 알림 가능 상태로 전환
            if (hasExitedRadius && wasInsideRadius) {
                android.util.Log.d("WalkVM", "비선호 강아지 $dogId 반경 이탈 (${dog.distanceM.toInt()}m) → 재알림 가능")
                currentStates[dogId] = alertState.copy(isInsideAlertRadius = false)
                continue
            }

            // 반경 진입 시 (새로 진입 or 쿨다운 후 재진입)
            if (isNowInsideRadius) {
                val timeSinceLastAlert = now - alertState.lastAlertTimeMs
                val isFirstEntry = !wasInsideRadius
                val cooldownPassed = timeSinceLastAlert >= ALERT_COOLDOWN_MS

                // 알림 발생 조건: 새로 진입 && 쿨다운 경과
                if (isFirstEntry && cooldownPassed) {
                    android.util.Log.d("WalkVM", "비선호 강아지 ${dog.name} 반경 진입 (${dog.distanceM.toInt()}m) → 알림 발생")

                    // 시스템 알림 발송
                    nearbyDogAlertManager.showNearbyDogAlert(dogId, dog.name, dog.distanceM)
                    // 워치에 비선호 강아지 알림 전송
                    viewModelScope.launch {
                        wearableManager.sendNotification("dog_warning", JSONObject().apply {
                            put("dogName", dog.name)
                            put("distance", dog.distanceM)
                        })
                    }

                    // 인앱 다이얼로그 표시 (첫 번째만)
                    if (newWarningDog == null) {
                        newWarningDog = dog
                    }

                    currentStates[dogId] = DogAlertState(
                        lastAlertTimeMs = now,
                        isInsideAlertRadius = true,
                    )
                } else if (isFirstEntry) {
                    // 쿨다운 중이면 반경 진입 상태만 업데이트 (알림 없음)
                    android.util.Log.d("WalkVM", "비선호 강아지 ${dog.name} 반경 진입 but 쿨다운 중 (${timeSinceLastAlert / 1000}초)")
                    currentStates[dogId] = alertState.copy(isInsideAlertRadius = true)
                }
                // 이미 반경 안에 있으면 아무 작업 안 함 (반복 알림 방지)
            }
        }

        // 3. 상태 업데이트
        _state.update { it.copy(dogAlertStates = currentStates) }

        // 4. 인앱 경고 다이얼로그 표시
        if (newWarningDog != null && _state.value.warningDog == null) {
            _state.update { it.copy(warningDog = newWarningDog) }
        }
    }

    /** 비선호 강아지 경고 다이얼로그 닫기 */
    fun dismissWarning() {
        _state.update { it.copy(warningDog = null) }
    }

    /** 알림 권한 필요 여부 확인 (Android 13+) */
    fun needsNotificationPermission(): Boolean {
        return nearbyDogAlertManager.needsNotificationPermission() &&
                !nearbyDogAlertManager.hasNotificationPermission()
    }

    /** 알림 권한 보유 여부 확인 */
    fun hasNotificationPermission(): Boolean {
        return nearbyDogAlertManager.hasNotificationPermission()
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

    // ── 위험장소 근접 알림 ───────────────────────────────────────────────────────

    /** 15초 간격 위험장소 폴링 시작 (산책 중에만 동작) */
    private fun startRiskZonePolling() {
        android.util.Log.d("WalkVM", "=== startRiskZonePolling 호출됨 ===")
        riskZonePollingJob?.cancel()
        riskZonePollingJob = viewModelScope.launch {
            while (true) {
                loadNearbyRiskZones()
                delay(RISK_ZONE_POLLING_INTERVAL_MS)
            }
        }
    }

    /** 위험장소 폴링 중단 */
    private fun stopRiskZonePolling() {
        riskZonePollingJob?.cancel()
        riskZonePollingJob = null
    }

    /** 현재 위치 기반 nearby 위험장소 조회 */
    private fun loadNearbyRiskZones() {
        val pos = _currentPosition.value
        if (pos == null) {
            android.util.Log.w("WalkVM", "loadNearbyRiskZones: 현재 위치 없음 (pos=null)")
            return
        }
        android.util.Log.d("WalkVM", "loadNearbyRiskZones: lat=${pos.latitude}, lon=${pos.longitude}, radius=$RISK_ZONE_QUERY_RADIUS_M")
        viewModelScope.launch {
            getNearbyMyRiskZonesUseCase(
                latitude = pos.latitude,
                longitude = pos.longitude,
                radiusMeters = RISK_ZONE_QUERY_RADIUS_M
            ).onSuccess { nearbyZones ->
                android.util.Log.d("WalkVM", "nearbyRiskZones 조회 성공: ${nearbyZones.size}개")
                nearbyZones.forEach { zone ->
                    android.util.Log.d("WalkVM", "  - id=${zone.id}, distanceM=${zone.distanceM}, reason=${zone.reason}")
                }
                _state.update { it.copy(nearbyDangerZones = nearbyZones) }
                processRiskZoneAlerts(nearbyZones)
            }.onFailure { e ->
                android.util.Log.w("WalkVM", "nearbyRiskZones 조회 실패: ${e.message}")
            }
        }
    }

    /**
     * 위험장소 근접 알림 로직
     * - distanceM <= 100m → 알림 발생 (enter) - 테스트: 100m
     * - distanceM > 150m 또는 목록에서 사라짐 → 다시 알림 가능 (exit)
     * - 같은 riskReportId에 대해 3분 쿨다운
     * - 반경 안에 머무는 동안 알림 반복하지 않음
     */
    private fun processRiskZoneAlerts(nearbyZones: List<NearbyDangerZone>) {
        android.util.Log.d("WalkVM", "processRiskZoneAlerts: ${nearbyZones.size}개 처리")
        if (nearbyZones.isEmpty()) {
            android.util.Log.d("WalkVM", "  → 근처 위험장소 없음, 스킵")
            return
        }
        val now = System.currentTimeMillis()
        val currentStates = _state.value.riskZoneAlertStates.toMutableMap()
        val nearbyZoneIds = nearbyZones.map { it.id }.toSet()

        // 1. 목록에서 사라진 위험장소 → 상태 초기화 (다시 알림 가능)
        val removedZoneIds = currentStates.keys - nearbyZoneIds
        removedZoneIds.forEach { zoneId ->
            android.util.Log.d("WalkVM", "위험장소 $zoneId 목록에서 사라짐 → 상태 초기화")
            currentStates.remove(zoneId)
        }

        // 2. 각 위험장소 처리 - 새로 알림할 것들을 리스트로 수집
        val newWarningZones = mutableListOf<NearbyDangerZone>()

        for (zone in nearbyZones) {
            val zoneId = zone.id
            val alertState = currentStates[zoneId] ?: RiskZoneAlertState()
            val wasInsideRadius = alertState.isInsideAlertRadius
            val isNowInsideRadius = zone.distanceM <= RISK_ZONE_ENTER_RADIUS_M
            val hasExitedRadius = zone.distanceM > RISK_ZONE_EXIT_RADIUS_M

            // 반경 이탈 시 → 다시 알림 가능 상태로 전환
            if (hasExitedRadius && wasInsideRadius) {
                android.util.Log.d("WalkVM", "위험장소 $zoneId 반경 이탈 (${zone.distanceM.toInt()}m) → 재알림 가능")
                currentStates[zoneId] = alertState.copy(isInsideAlertRadius = false)
                continue
            }

            // 반경 진입 시 (새로 진입 or 쿨다운 후 재진입)
            if (isNowInsideRadius) {
                val timeSinceLastAlert = now - alertState.lastAlertTimeMs
                val isFirstEntry = !wasInsideRadius
                val cooldownPassed = timeSinceLastAlert >= RISK_ZONE_COOLDOWN_MS

                // 알림 발생 조건: 새로 진입 && 쿨다운 경과
                if (isFirstEntry && cooldownPassed) {
                    android.util.Log.d("WalkVM", "위험장소 $zoneId 반경 진입 (${zone.distanceM.toInt()}m) → 알림 발생")

                    // 시스템 알림 발송
                    riskZoneAlertManager.showRiskZoneAlert(zoneId, zone.distanceM)

                    // 인앱 다이얼로그 큐에 추가
                    newWarningZones.add(zone)

                    currentStates[zoneId] = RiskZoneAlertState(
                        lastAlertTimeMs = now,
                        isInsideAlertRadius = true,
                    )
                } else if (isFirstEntry) {
                    // 쿨다운 중이면 반경 진입 상태만 업데이트 (알림 없음)
                    android.util.Log.d("WalkVM", "위험장소 $zoneId 반경 진입 but 쿨다운 중 (${timeSinceLastAlert / 1000}초)")
                    currentStates[zoneId] = alertState.copy(isInsideAlertRadius = true)
                }
                // 이미 반경 안에 있으면 아무 작업 안 함 (반복 알림 방지)
            }
        }

        // 3. 상태 업데이트
        _state.update { it.copy(riskZoneAlertStates = currentStates) }

        // 4. 인앱 경고 다이얼로그 큐에 추가 (거리순 정렬)
        if (newWarningZones.isNotEmpty()) {
            val sortedNewZones = newWarningZones.sortedBy { it.distanceM }
            _state.update { current ->
                // 기존 큐에 새로운 것들 추가 (중복 제거)
                val existingIds = current.warningRiskZoneQueue.map { it.id }.toSet()
                val toAdd = sortedNewZones.filter { it.id !in existingIds }
                current.copy(warningRiskZoneQueue = current.warningRiskZoneQueue + toAdd)
            }
        }
    }

    /** 위험장소 경고 다이얼로그 닫기 (큐에서 첫 번째 제거) */
    fun dismissRiskZoneWarning() {
        _state.update { current ->
            current.copy(warningRiskZoneQueue = current.warningRiskZoneQueue.drop(1))
        }
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
        // optimistic: 목록에서 제거 + 수락자 확인 모달 표시
        _state.update { it.copy(
            pendingProposals = it.pendingProposals.filter { p -> p.proposalId != proposal.proposalId },
            showAcceptedByMeDialog = true,
        ) }
        viewModelScope.launch {
            walkRepository.respondToProposal(proposal.proposalId, "ACCEPT", myWalkRecordId)
                .onSuccess { chatRoomId ->
                    if (chatRoomId != null) {
                        // 수락자 측: chatRoomId 저장 + WebSocket 구독
                        _state.update { it.copy(
                            acceptedChatRooms = it.acceptedChatRooms + (proposal.dogId to chatRoomId),
                            acceptedByMeChatRoomId = chatRoomId,
                        ) }
                        subscribeToChatRoom(chatRoomId, proposal.name, proposal.profileImageUrl)
                    }
                }
        }
    }

    /** 받은 제안 거절 */
    fun rejectProposal(proposal: PendingProposalInfo) {
        val myWalkRecordId = currentWalkId ?: return
        // optimistic: 목록에서 제거 + 거절자 확인 모달 표시
        _state.update { it.copy(
            pendingProposals = it.pendingProposals.filter { p -> p.proposalId != proposal.proposalId },
            showRejectedByMeDialog = true,
        ) }
        viewModelScope.launch {
            walkRepository.respondToProposal(proposal.proposalId, "REJECT", myWalkRecordId)
        }
    }

    /** 제안자 — 수락 알림 확인 */
    fun dismissAcceptedProposal(proposalId: String) {
        val accepted = _state.value.acceptedProposals.find { it.proposalId == proposalId }
        _state.update { it.copy(
            acceptedProposals = it.acceptedProposals.filter { a -> a.proposalId != proposalId }
        ) }
        // 제안자 측: chatRoomId 저장 + WebSocket 구독
        accepted?.chatRoomId?.let { chatRoomId ->
            _state.update { it.copy(
                acceptedChatRooms = it.acceptedChatRooms + (accepted.dogId to chatRoomId)
            ) }
            subscribeToChatRoom(chatRoomId, accepted.name, accepted.profileImageUrl)
        }
    }

    /** 제안자 — 거절 알림 확인 */
    fun dismissRejectedProposal(proposalId: String) {
        _state.update { it.copy(
            rejectedProposals = it.rejectedProposals.filter { r -> r.proposalId != proposalId }
        ) }
    }

    /** 수락자 — 수락 확인 모달 닫기 */
    fun dismissAcceptedByMe() {
        _state.update { it.copy(showAcceptedByMeDialog = false) }
    }

    /** 거절자 — 거절 확인 모달 닫기 */
    fun dismissRejectedByMe() {
        _state.update { it.copy(showRejectedByMeDialog = false) }
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

    /** GPS 위치 업데이트 시 위험구역 근접 여부 체크 → 워치 알림 전송 */
    private fun checkDangerZoneProximity(lat: Double, lon: Double) {
        val now = System.currentTimeMillis()
        val zones = _state.value.dangerZones
        for (zone in zones) {
            val distM = haversineMeters(lat, lon, zone.location.latitude, zone.location.longitude)
            if (distM <= DANGER_ZONE_ALERT_RADIUS_M) {
                val lastAlert = dangerZoneAlertTimes[zone.id] ?: 0L
                if (now - lastAlert >= DANGER_ZONE_COOLDOWN_MS) {
                    dangerZoneAlertTimes[zone.id] = now
                    val reason = zone.customReason ?: zone.reason.label
                    viewModelScope.launch {
                        wearableManager.sendNotification("danger_zone", JSONObject().apply {
                            put("reason", reason)
                        })
                    }
                    android.util.Log.d("WalkVM", "위험구역 근접 알림 전송: id=${zone.id}, reason=$reason, dist=${distM.toInt()}m")
                }
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

    /**
     * 위험 구역 신고 제출
     * - 서버 실패 시 Result.failure 반환 (실패를 삼키지 않음)
     * - 성공 시 mine 목록 재조회하여 persisted 상태 동기화
     */
    fun submitDangerReport() {
        val location = _state.value.selectedLocation ?: return
        val reason = _state.value.selectedDangerReason ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val customReason = if (reason == DangerReason.OTHER) {
                _state.value.customDangerReason.takeIf { it.isNotBlank() }
            } else null

            reportDangerZoneUseCase(
                walkId = currentWalkId,
                location = location,
                reason = reason,
                customReason = customReason
            ).onSuccess { result ->
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
                // 신고 성공 후 mine 목록 재조회하여 persisted 상태 동기화
                loadPersistedDangerZones()
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "위험장소 신고 실패: ${e.message}"
                    )
                }
            }
        }
    }

    // ── 발자국 찍기 ────────────────────────────────────────────────────────────

    /** 발자국 감지용 주변 장소 로드 (산책 시작 시 1회) — 이미 도장 찍은 장소 제외 */
    private fun loadWalkPlaces(latitude: Double, longitude:Double) {
        val dogId = _state.value.myDogId ?: return
        viewModelScope.launch {
            val stampedIds = getFootprintPlacesUseCase(dogId)
                .getOrDefault(emptyList())
                .map { it.id }
                .toSet()
            getPlacesUseCase(latitude, longitude, radius = 500.0).onSuccess { places ->
                _state.update { it.copy(walkPlaces = places.filter { it.id !in stampedIds }) }
            }
        }
    }

    /**
     * 20m 진입/이탈 기반 발자국 알림 처리
     * - 20m 진입 시: 알림 + 오버레이 표시
     * - 20m 이탈 시: 알림 취소 + 오버레이 닫기
     * - 20m 재진입 시: 알림 + 오버레이 다시 표시
     * - 도장 찍은 장소: hasStamped=true로 영구 무시
     */
    private fun checkNearbyPlacesForFootprint(currentPos: LatLng) {
        val places = _state.value.walkPlaces
        if (places.isEmpty()) return

        val prevStates = _state.value.footprintAlertStates
        val newStates = prevStates.toMutableMap()
        val currentAlertPlace = _state.value.footprintAlertPlace

        // 1단계: 모든 장소의 반경 내/외 상태 갱신
        for (place in places) {
            val prev = prevStates[place.id] ?: FootprintAlertState()
            if (prev.hasStamped) continue
            val dist = haversineMeters(
                currentPos.latitude, currentPos.longitude,
                place.latitude, place.longitude
            )
            newStates[place.id] = prev.copy(isInsideRadius = dist <= FOOTPRINT_ALERT_RADIUS_M)
        }

        // 2단계: 현재 오버레이 중인 장소가 반경을 이탈했으면 닫기
        var newAlertPlace = currentAlertPlace
        if (currentAlertPlace != null && newStates[currentAlertPlace.id]?.isInsideRadius != true) {
            newAlertPlace = null
            footprintAlertManager.cancelAlert()
        }

        // 3단계: 오버레이 없는 상태에서 새로 20m 진입한 장소 탐색
        if (newAlertPlace == null) {
            for (place in places) {
                val prev = prevStates[place.id] ?: FootprintAlertState()
                val next = newStates[place.id] ?: FootprintAlertState()
                if (next.hasStamped || !next.isInsideRadius) continue
                // 이번에 새로 진입한 경우에만 알림 발송
                if (!prev.isInsideRadius) {
                    footprintAlertManager.showFootprintAlert(place.name)
                    // 워치에 발자국 알림 전송
                    val wId = currentWalkId
                    val dId = _state.value.myDogId
                    if (wId != null && dId != null) {
                        viewModelScope.launch {
                            wearableManager.sendInteractiveMessage("footprint", JSONObject().apply {
                                put("placeId", place.id)
                                put("placeName", place.name)
                                put("walkId", wId)
                                put("dogId", dId)
                            })
                        }
                    }
                }
                newAlertPlace = place
                break
            }
        }

        _state.update {
            it.copy(
                footprintAlertStates = newStates,
                footprintAlertPlace = newAlertPlace,
                // 오버레이가 사라지면 stamped 상태도 초기화
                footprintStamped = if (newAlertPlace == null) false else it.footprintStamped,
                // 20m 오버레이가 뜨면 50m 배너 숨기기
                nearbyStampablePlace = if (newAlertPlace != null) null else it.nearbyStampablePlace,
            )
        }
    }

    /** 발자국 도장 찍기 — 해당 장소를 이번 산책 내내 무시(hasStamped=true) */
    fun stampFootprint() {
        val place = _state.value.footprintAlertPlace ?: return
        val walkId = currentWalkId ?: return
        val dogId = _state.value.myDogId ?: return

        _state.update { state ->
            val newStates = state.footprintAlertStates.toMutableMap()
            newStates[place.id] = FootprintAlertState(isInsideRadius = true, hasStamped = true)
            state.copy(
                footprintStamped = true,
                footprintAlertStates = newStates,
                stampedPlaceIds = state.stampedPlaceIds + place.id,
            )
        }
        viewModelScope.launch {
            stampPlaceUseCase(walkId, dogId, place.id)
                .onFailure { e ->
                    android.util.Log.w("WalkVM", "발자국 도장 찍기 실패: ${e.message}")
                }
        }
    }

    /** 발자국 오버레이 닫기 (2초 자동 닫힘 후 호출) */
    fun dismissFootprintOverlay() {
        _state.update { it.copy(footprintAlertPlace = null, footprintStamped = false) }
    }

    // ── 채팅 관련 ─────────────────────────────────────────────────────────────

    /** WebSocket 연결 후 특정 채팅방 구독 + 메시지 수신 시 배너 표시 */
    private fun subscribeToChatRoom(
        chatRoomId: Long,
        partnerName: String,
        partnerImageUrl: String?,
    ) {
        viewModelScope.launch {
            val token = tokenDataStore.getAccessToken().first() ?: return@launch
            val wsUrl = buildWsUrl()
            if (!stompChatClient.isConnected) {
                stompChatClient.connect(wsUrl, token)
                // CONNECTED 신호 대기 (최대 15초) — 연결 실패 시 무한 대기 방지
                val connected = withTimeoutOrNull(15_000L) {
                    stompChatClient.connected.first { it }
                }
                if (connected == null) return@launch
            }
            stompChatClient.subscribe(chatRoomId)

            // 수신 메시지 → 배너 알림 (ChatScreen에서 이미 보고 있으면 억제)
            stompChatClient.messages.collect { (roomId, message) ->
                if (roomId == chatRoomId && stompChatClient.activeChatRoomId != chatRoomId) {
                    _state.update { it.copy(
                        chatBanner = ChatBannerNotification(
                            chatRoomId = roomId,
                            senderName = partnerName,
                            senderImageUrl = partnerImageUrl,
                            messagePreview = message.content.take(40),
                        )
                    ) }
                }
            }
        }
    }

    /** 채팅 배너 닫기 */
    fun dismissChatBanner() {
        _state.update { it.copy(chatBanner = null) }
    }

    private fun buildWsUrl(): String {
        // BASE_URL 예: "http://192.168.30.183:8080/api/v1/"
        // WS URL 결과: "ws://192.168.30.183:8080/api/v1/ws-native"
        val scheme = if (com.frontend.util.Constants.BASE_URL.startsWith("https")) "wss" else "ws"
        val base = com.frontend.util.Constants.BASE_URL
            .removePrefix("https://").removePrefix("http://")
            .trimEnd('/')
        return "$scheme://$base/ws-native"
    }
}
