package com.frontend.ui.screen.walk

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.frontend.domain.model.WalkRoute
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
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class WalkViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(WalkState())
    val state = _state.asStateFlow()

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

    fun showFilter() {
        _state.update { it.copy(showFilterSheet = true) }
    }

    fun hideFilter() {
        _state.update { it.copy(showFilterSheet = false) }
    }

    fun selectFilter(filter: WalkFilterType) {
        _state.update { it.copy(selectedFilter = filter) }
    }

    fun applyFilter() {
        _state.update { it.copy(showFilterSheet = false) }
    }
}
