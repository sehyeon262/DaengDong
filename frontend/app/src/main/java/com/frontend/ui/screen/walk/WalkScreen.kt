package com.frontend.ui.screen.walk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.frontend.R
import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerZone
import com.frontend.domain.model.NearbyDangerZone
import com.frontend.domain.model.NearbyDogResponse
import com.frontend.domain.model.PersistedDangerZone
import com.frontend.ui.component.MapOverlayButton
import com.frontend.ui.screen.walk.components.DangerReportModal
import com.frontend.ui.screen.walk.components.DogWarningDialog
import com.frontend.ui.screen.walk.components.RiskZoneWarningDialog
import com.frontend.ui.screen.walk.components.NearbyDogProfilePopup
import com.frontend.ui.screen.walk.components.PlaceDetailBottomSheet
import com.frontend.ui.screen.walk.components.ProposalAcceptedByMeDialog
import com.frontend.ui.screen.walk.components.ProposalAcceptedDialog
import com.frontend.ui.screen.walk.components.ProposalIncomingDialog
import com.frontend.ui.screen.walk.components.ProposalRejectedByMeDialog
import com.frontend.ui.screen.walk.components.ProposalRejectedDialog
import com.frontend.ui.screen.walk.components.WalkFilterBottomSheet
import com.frontend.ui.screen.walk.components.WalkRouteCard
import com.frontend.ui.screen.walk.components.WalkSearchBar
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextMain
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import androidx.core.graphics.scale
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.shape.MapPoints
import com.kakao.vectormap.shape.Polygon
import com.kakao.vectormap.shape.PolygonOptions
import com.kakao.vectormap.shape.PolygonStyle
import com.kakao.vectormap.shape.Polyline
import com.kakao.vectormap.shape.PolylineOptions
import com.kakao.vectormap.shape.PolylineStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkScreen(
    refreshRequestKey: Long = 0L,
    onNavigateToRecord: () -> Unit = {},
    onNavigateToWalkDetail: (Long) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToAddPlace: () -> Unit = {},
    onNavigateToChat: (Long) -> Unit = {},
    viewModel: WalkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val routes = viewModel.getDisplayRoutes()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val azimuth by viewModel.azimuth.collectAsState()

    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var currentLocationLabel by remember { mutableStateOf<Label?>(null) }
    val currentPosition by viewModel.currentPosition.collectAsState()
    var hasLoadedInitialRoutes by remember { mutableStateOf(false) }
    var lastHandledRefreshRequestKey by remember { mutableStateOf(0L) }
    var fovOverlay by remember { mutableStateOf<Polygon?>(null) }

    // 산책 경로 폴리라인 (실제 산책 중 GPS 트래킹)
    val routePoints by viewModel.routePoints.collectAsState()
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }

    // 추천 경로 미리보기 폴리라인
    var recommendedRoutePolyline by remember { mutableStateOf<Polyline?>(null) }

    // 위험 구역 마커 목록 (강아지 마커와 분리 관리)
    val dangerZoneLabels = remember { mutableStateListOf<Label>() }

    // 장소 마커 목록 (PLACE 필터 on/off 시 추가/제거)
    val placeLabels = remember { mutableStateListOf<Label>() }

    // 발자국 마커 목록 (FOOTPRINT 필터 on/off 시 추가/제거)
    val footprintLabels = remember { mutableStateListOf<Label>() }

    // 주변 강아지 마커 목록
    val nearbyDogLabels = remember { mutableStateListOf<Label>() }

    // 위치 권한 요청 launcher - 허용 시 위치 트래킹 시작
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.startLocationTracking()
        }
    }

    // 미디어 읽기 권한 요청 launcher (산책 중 카메라 사진 자동 감지용)
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.retryPhotoObserverIfWalking()
        } else {
            // Android 14+에서 "사진 선택"(부분 접근)을 선택한 경우
            // 자동 감지는 불가하지만 산책 후 수동 업로드는 가능
            android.util.Log.w("WalkScreen", "사진 전체 접근 미허용 — 자동 감지 비활성화 (수동 업로드 가능)")
        }
    }

    // 알림 권한 요청 launcher (Android 13+ 비선호 강아지 알림용)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        android.util.Log.d("WalkScreen", "알림 권한 요청 결과: $granted")
    }

    // 산책 시작 시 미디어 권한 + 알림 권한 확인 및 요청
    LaunchedEffect(state.isWalking) {
        if (state.isWalking) {
            // 자동 감지에는 전체 접근(READ_MEDIA_IMAGES) 필요
            // Android 14+ "사진 선택"(부분 접근)으로는 새 카메라 사진을 MediaStore로 감지 불가
            val hasFullAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }

            if (!hasFullAccess) {
                val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                Unit
            }

            // 알림 권한 요청 (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (viewModel.needsNotificationPermission()) {
                    Unit
                }
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = state.selectedRouteIndex,
        pageCount = { routes.size }
    )

    // 페이저 페이지 변경 시 ViewModel 상태 동기화
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.selectRoute(page)
        }
    }

    // 산책 시작 실패 시 Toast로 오류 메시지 표시
    LaunchedEffect(state.walkError) {
        val error = state.walkError ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
    }

    // 지도 준비 완료 시 위치 트래킹 시작
    LaunchedEffect(kakaoMap) {
        if (kakaoMap == null) return@LaunchedEffect
        val hasPermission =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.startLocationTracking()
        }
    }

    // 위치 변경 시 마커 + FOV cone 갱신
    LaunchedEffect(currentPosition, kakaoMap) {
        val pos = currentPosition ?: return@LaunchedEffect
        val map = kakaoMap ?: return@LaunchedEffect

        if (currentLocationLabel == null) {
            // 최초: 카메라 이동 + 마커 생성 (트래킹은 GPS 버튼으로만 활성화)
            map.moveCamera(CameraUpdateFactory.newCenterPosition(pos, 15))
            val bitmap = rotateBitmap(createDogMarkerBitmap(context), azimuth)
            val styles = LabelStyles.from(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f))
            val label = map.labelManager?.layer?.addLabel(LabelOptions.from(pos).setStyles(styles))
            currentLocationLabel = label

            // 최초 위치 수신 시 추천 경로 로드
        } else {
            // 이후: moveTo()로 이동 (마커 사라짐 없이, 카메라는 자유이동 유지)
            currentLocationLabel?.moveTo(pos)
        }

        // FOV cone 갱신
        fovOverlay = updateFovCone(map, pos, azimuth, fovOverlay)
    }

    // azimuth 변경 시 강아지 마커 회전 + FOV cone 업데이트
    // Refresh recommendations only when the user taps the walk tab again.
    LaunchedEffect(currentPosition, refreshRequestKey) {
        val pos = currentPosition ?: return@LaunchedEffect
        when {
            refreshRequestKey > lastHandledRefreshRequestKey -> {
                lastHandledRefreshRequestKey = refreshRequestKey
                if (!state.isWalking) {
                    viewModel.refreshRecommendedRoutes(pos.latitude, pos.longitude)
                    hasLoadedInitialRoutes = true
                }
            }
            !hasLoadedInitialRoutes -> {
                viewModel.loadRecommendedRoutes(pos.latitude, pos.longitude)
                hasLoadedInitialRoutes = true
            }
        }
    }

    LaunchedEffect(azimuth) {
        val pos = currentPosition ?: return@LaunchedEffect
        val map = kakaoMap ?: return@LaunchedEffect

        // changeStyles()로 비트맵만 교체 (마커 사라짐 없이)
        val bitmap = rotateBitmap(createDogMarkerBitmap(context), azimuth)
        val styles = LabelStyles.from(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f))
        currentLocationLabel?.changeStyles(styles)

        // FOV cone 업데이트
        fovOverlay = updateFovCone(map, pos, azimuth, fovOverlay)
    }

    // 외부에서 selectedRouteIndex 변경 시 페이저 스크롤
    LaunchedEffect(state.selectedRouteIndex) {
        if (pagerState.currentPage != state.selectedRouteIndex) {
            pagerState.animateScrollToPage(state.selectedRouteIndex)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
    // 위험 구역 선택 모드 진입 시 TrackingManager 중단
    // (트래킹은 GPS 버튼 클릭 시에만 일시적으로 카메라 이동 — startTracking 사용 안 함)
    LaunchedEffect(state.isSelectingDangerZone, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (state.isSelectingDangerZone) {
            map.trackingManager?.stopTracking()
        }
        // 선택 모드 해제 시에도 자동 트래킹 재개 안 함 → 지도 자유 이동 유지
    }

    // 주변 강아지 마커: NEARBY_DOG 필터 활성화 시에만 표시
    // 알림 기능은 필터와 무관하게 ViewModel에서 처리
    // avoidAlertCandidate 마커는 생성 시 강조된 글로우 효과 적용 (addNearbyDogMarker에서 처리)
    LaunchedEffect(state.nearbyDogs, state.activeFilters, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect

        // 기존 마커 전부 제거
        nearbyDogLabels.forEach { it.remove() }
        nearbyDogLabels.clear()

        // NEARBY_DOG 필터가 활성화된 경우에만 마커 표시
        if (WalkFilterType.NEARBY_DOG !in state.activeFilters) {
            return@LaunchedEffect
        }

        state.nearbyDogs.forEach { dog ->
            val label = addNearbyDogMarker(context, map, dog)
            if (label != null) nearbyDogLabels.add(label)
        }
    }

    // 비선호 강아지 마커 pulse 애니메이션
    // KakaoMap Label 런타임 스타일 변경을 통해 구현
    val avoidAlertDogIds = remember(state.nearbyDogs) {
        state.nearbyDogs.filter { it.avoidAlertCandidate }.map { it.dogId }.toSet()
    }

    // Pulse 애니메이션 (2초 주기, scale 0.95 ~ 1.05)
    LaunchedEffect(avoidAlertDogIds, state.activeFilters, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (WalkFilterType.NEARBY_DOG !in state.activeFilters || avoidAlertDogIds.isEmpty()) {
            return@LaunchedEffect
        }

        // 천천히 왕복하는 pulse 효과 (2초 사이클)
        var phase = 0
        while (true) {
            kotlinx.coroutines.delay(50L)  // 20fps
            phase = (phase + 1) % 40  // 40 frames = 2초

            // sine wave로 부드러운 scale 변화: 0.92 ~ 1.08
            val scale = 1.0f + 0.08f * kotlin.math.sin(phase * kotlin.math.PI.toFloat() / 20f)

            // avoidAlertCandidate 마커 스타일 업데이트
            nearbyDogLabels.forEach { label ->
                val dog = label.tag as? NearbyDogResponse ?: return@forEach
                if (dog.avoidAlertCandidate) {
                    try {
                        // 마커 비트맵 재생성하여 스타일 적용
                        val markerSize = (80 * scale).toInt()
                        val bitmap = createPulsingAvoidMarkerBitmap(context, dog, markerSize)
                        val style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)
                        val styles = LabelStyles.from(style)
                        label.changeStyles(styles)
                    } catch (e: Exception) {
                        // 스타일 변경 실패 시 무시 (마커가 제거된 경우 등)
                    }
                }
            }
        }
    }

    // 위험 구역 마커 동적 렌더링 (persistedDangerZones + nearbyDangerZones id 기준 dedupe)
    LaunchedEffect(state.persistedDangerZones, state.nearbyDangerZones, state.dangerZones, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        val labelLayer = map.labelManager?.layer ?: return@LaunchedEffect

        // 기존 마커 모두 제거
        dangerZoneLabels.forEach { label ->
            try {
                labelLayer.remove(label)
            } catch (_: Exception) {}
        }
        dangerZoneLabels.clear()

        // persistedDangerZones를 DangerZone으로 변환
        val persistedAsZones = state.persistedDangerZones.map { persisted ->
            DangerZone(
                id = persisted.id,
                location = persisted.location,
                reason = persisted.reason,
                customReason = persisted.customReason
            )
        }

        // nearbyDangerZones를 DangerZone으로 변환
        val nearbyAsZones = state.nearbyDangerZones.map { nearby ->
            DangerZone(
                id = nearby.id,
                location = nearby.location,
                reason = nearby.reason,
                customReason = nearby.customReason
            )
        }

        // 세션 신고 + persisted + nearby 합치고 id 기준 dedupe
        val allZones = (state.dangerZones + persistedAsZones + nearbyAsZones)
            .distinctBy { it.id }

        // 새 마커 추가
        allZones.forEach { zone ->
            val label = addDangerZoneMarker(context, map, zone)
            if (label != null) dangerZoneLabels.add(label)
        }
    }

    // 산책 경로 폴리라인 실시간 업데이트 (GPS 트래킹)
    LaunchedEffect(routePoints, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect

        // 경로가 없으면 기존 폴리라인 제거 (산책 종료 후 지도 초기화)
        if (routePoints.size < 2) {
            routePolyline?.let {
                map.shapeManager?.layer?.remove(it)
                routePolyline = null
            }
            return@LaunchedEffect
        }

        val mapPoints = MapPoints.fromLatLng(routePoints)
        if (routePolyline != null) {
            // 기존 폴리라인 좌표만 갱신 (재생성 없이 → 깜빡임 방지)
            routePolyline?.changeMapPoints(listOf(mapPoints))
        } else {
            // 첫 생성: from(lineWidth, color) 순서 주의
            val style = PolylineStyle.from(
                15f,                                             // lineWidth
                android.graphics.Color.argb(220, 240, 216, 80)  // 반투명 #F0D850
            )
            routePolyline = map.shapeManager?.layer?.addPolyline(
                PolylineOptions.from(mapPoints, style)
            )
        }
    }

    // 추천 경로 미리보기 폴리라인 (선택된 경로 변경 시 업데이트)
    LaunchedEffect(state.selectedRouteIndex, state.recommendedRoutes, state.showRecommendedRoute, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect

        fun clearRecommendedRoutePreview() {
            recommendedRoutePolyline?.let {
                map.shapeManager?.layer?.remove(it)
                recommendedRoutePolyline = null
            }
        }

        // 경로 표시 OFF이면 폴리라인 제거
        if (!state.showRecommendedRoute) {
            clearRecommendedRoutePreview()
            return@LaunchedEffect
        }

        // 선택된 추천 경로 가져오기 (자유 산책이면 null)
        val selectedRoute = viewModel.getSelectedRecommendedRoute()

        // 추천 경로가 없으면 폴리라인 제거
        if (selectedRoute == null) {
            clearRecommendedRoutePreview()
            return@LaunchedEffect
        }

        // 경로 포인트 가져오기 (actualPathPoints 우선, 없으면 polyline)
        val pathPoints = selectedRoute.getPathPoints()
        if (pathPoints.size < 2) {
            clearRecommendedRoutePreview()
            return@LaunchedEffect
        }

        // LatLngPoint를 KakaoMap LatLng로 변환
        val latLngList = pathPoints.map { LatLng.from(it.latitude, it.longitude) }
        val mapPoints = MapPoints.fromLatLng(latLngList)

        // 기존 폴리라인 제거 후 새로 생성 (경로 전환 시 깔끔하게)
        clearRecommendedRoutePreview()

        // 추천 경로 스타일: 파란색 점선 느낌
        val style = PolylineStyle.from(
            12f,                                             // lineWidth
            android.graphics.Color.argb(200, 66, 133, 244)  // 파란색 #4285F4
        )
        recommendedRoutePolyline = map.shapeManager?.layer?.addPolyline(
            PolylineOptions.from(mapPoints, style)
        )

    }

    // 장소 목록 변경 시: 마커 전체 교체 (PLACE 필터 ON → API 응답 도착)
    // 발자국 찍은 장소는 dog_footprint 마커로 대체되므로 place_mark 마커 제외
    LaunchedEffect(state.places, state.footprintPlaces, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        placeLabels.forEach { map.labelManager?.layer?.remove(it) }
        placeLabels.clear()
        if (state.places.isEmpty()) return@LaunchedEffect
        val footprintPlaceIds = state.footprintPlaces.map { it.id }.toSet()
        state.places.forEach { place ->
            if (place.id !in footprintPlaceIds) {
                val label = addPlaceMarker(context, map, place)
                if (label != null) placeLabels.add(label)
            }
        }
    }

    // PLACE 필터 ON/OFF 처리
    LaunchedEffect(state.activeFilters, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (WalkFilterType.PLACE in state.activeFilters) {
            val center = map.cameraPosition?.position ?: return@LaunchedEffect
            viewModel.loadPlacesByPosition(center.latitude, center.longitude)
        } else {
            placeLabels.forEach { map.labelManager?.layer?.remove(it) }
            placeLabels.clear()
        }
    }

    // 발자국 목록 변경 시: 마커 전체 교체 (FOOTPRINT 필터 ON → API 응답 도착)
    LaunchedEffect(state.footprintPlaces, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        footprintLabels.forEach { map.labelManager?.layer?.remove(it) }
        footprintLabels.clear()
        if (state.footprintPlaces.isEmpty()) return@LaunchedEffect
        state.footprintPlaces.forEach { place ->
            val label = addFootprintMarker(context, map, place)
            if (label != null) footprintLabels.add(label)
        }
    }

    // 카메라 이동 완료 시 장소 재조회 + 마커 클릭 리스너 등록
    LaunchedEffect(kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        // 지도 이동 시 PLACE 필터 ON이면 새 중심 좌표로 장소 재조회
        map.setOnCameraMoveEndListener { _, cameraPosition, _ ->
            if (WalkFilterType.PLACE in viewModel.state.value.activeFilters) {
                val center = cameraPosition.position
                viewModel.loadPlacesByPosition(center.latitude, center.longitude)
            }
        }
        // 마커 클릭 시 처리 (주변 강아지 / 장소 구분)
        map.setOnLabelClickListener { _, _, label ->
            val dog = label.tag as? NearbyDogResponse
            if (dog != null) {
                viewModel.selectNearbyDog(dog)
                return@setOnLabelClickListener true
            }
            val placeId = label.tag as? Long
            val place = viewModel.state.value.places.find { it.id == placeId }
            if (place != null) viewModel.selectPlace(place)
            true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. 카카오맵 ─────────────────────────────────────────────────
        KakaoMapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map -> kakaoMap = map }
        )

        // ── 2. 위험 구역 선택 모드: 지도 중앙 깃발 핀 오버레이 ──────────
        // fillMaxSize Box 없이 외부 Box의 align으로만 배치 → 지도 터치 통과
        if (state.isSelectingDangerZone) {
            Image(
                painter = painterResource(id = R.drawable.danger_flag),
                contentDescription = "위험 위치 선택 핀",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(72.dp)
                    .offset(y = (-36).dp)   // 깃발 하단이 지도 좌표에 맞도록
                    .align(Alignment.Center)
            )
        }

        // ── 발자국 찍기 오버레이 ──────────────────────────────────────────
        if (state.footprintAlertPlace != null && !state.isSelectingDangerZone) {
            FootprintStampOverlay(
                stamped = state.footprintStamped,
                onTap = { viewModel.stampFootprint() }
            )
            if (state.footprintStamped) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000L)
                    viewModel.dismissFootprintOverlay()
                }
            }
        }

        // ── 3. 전체 오버레이 레이아웃 (검색바 + 하단 패널) ───────────
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 검색바
            WalkSearchBar(
                onBackClick = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // 위험 구역 선택 모드: 상단 안내 배너
            if (state.isSelectingDangerZone) {
                DangerZoneSelectionBanner(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 48.dp)
                )
            }

            // 발자국 도장 프롬프트 (산책 중 50m 이내 장소 감지 시)
            val stampablePlace = state.nearbyStampablePlace
            if (state.isWalking && stampablePlace != null) {
                FootprintStampBanner(
                    placeName = stampablePlace.name,
                    onDismiss = { viewModel.dismissStampPrompt() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 위험 구역 선택 모드: 하단 "선택하기" 패널
            if (state.isSelectingDangerZone) {
                DangerZoneBottomPanel(
                    onSelectClick = {
                        // 지도 중심 좌표 캡처
                        val center = kakaoMap?.cameraPosition?.position
                        viewModel.selectDangerLocation(
                            DangerLocation(
                                latitude = center?.latitude ?: 37.5665,
                                longitude = center?.longitude ?: 126.9780
                            )
                        )
                        viewModel.openDangerReportDialog()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                )
            } else if (state.isWalking) {
                // 산책 중 모드: 통계 패널 + 일시정지/종료 버튼
                WalkingStatsPanel(
                    elapsedSeconds = state.elapsedSeconds,
                    distanceMeters = state.distanceMeters,
                    isPaused = state.isPaused,
                    onPauseResume = {
                        if (state.isPaused) viewModel.resumeWalk() else viewModel.pauseWalk()
                    },
                    onEndWalk = { viewModel.endWalk() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                // 일반 모드: 경로 추천 + 산책 시작 패널
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    // "추천 산책 경로" 레이블
                    Text(
                        text = "추천 산책 경로",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                        modifier = Modifier.padding(start = 20.dp, bottom = 10.dp)
                    )

                    // 경로 카드 가로 페이저
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(start = 20.dp, end = screenWidth * 0.44f),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { pageIndex ->
                        WalkRouteCard(
                            route = routes[pageIndex],
                            isSelected = pageIndex == state.selectedRouteIndex,
                            onClick = { viewModel.selectRoute(pageIndex) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 산책 시작 버튼
                    Button(
                        onClick = { viewModel.startFreeWalk() },
                        enabled = !state.isWalking && !state.isRoutesLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight * 0.067f)
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PointGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "산책 시작",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── 4. 우측 플로팅 버튼 (위험신고, 현재위치, 필터) ──────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = screenHeight * 0.225f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 위험 구역 신고 FAB (토글)
            MapOverlayButton(
                painter = painterResource(id = R.drawable.alert),
                contentDescription = if (state.isSelectingDangerZone) "위험 신고 취소" else "위험 구역 신고",
                onClick = {
                    if (state.isSelectingDangerZone) viewModel.cancelDangerZoneSelection()
                    else viewModel.startDangerZoneSelection()
                }
            )
            MapOverlayButton(
                icon = Icons.Filled.GpsFixed,
                contentDescription = "현재 위치",
                onClick = {
                    // 현재 위치로 카메라 이동 (한 번만, 이후 지도 자유이동 유지)
                    currentPosition?.let { pos ->
                        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(pos, 15))
                    }
                }
            )
            // 추천 경로 표시 토글 버튼
            MapOverlayButton(
                icon = if (state.showRecommendedRoute) Icons.Filled.Route else Icons.Outlined.Route,
                contentDescription = if (state.showRecommendedRoute) "경로 숨기기" else "경로 보기",
                onClick = { viewModel.toggleRecommendedRouteVisibility() }
            )
            MapOverlayButton(
                icon = Icons.Filled.FilterAlt,
                contentDescription = "필터",
                onClick = { viewModel.showFilter() }
            )
            // 신규 장소 등록 버튼
            MapOverlayButton(
                icon = Icons.Filled.Add,
                contentDescription = "장소 추가",
                onClick = { onNavigateToAddPlace() }
            )
        }

        // ── 5. `필터 바텀`시트 ────────────────────────────────────────────
        if (state.showFilterSheet) {
            WalkFilterBottomSheet(
                activeFilters = state.pendingFilters,
                onFilterToggle = { viewModel.toggleFilter(it) },
                onApply = { viewModel.applyFilter() },
                onDismiss = { viewModel.hideFilter() }
            )
        }

        // ── 7. 장소 상세 바텀시트 ───────────────────────────────────────
        state.selectedPlace?.let { place ->
            PlaceDetailBottomSheet(
                place = place,
                onDismiss = { viewModel.dismissPlaceDetail() }
            )
        }

        // ── 8. 주변 강아지 공개 프로필 팝업 ─────────────────────────────
        state.selectedNearbyDog?.let { dog ->
            NearbyDogProfilePopup(
                nearbyDog = dog,
                profile = state.dogPublicProfile,
                isLoading = state.isDogProfileLoading,
                proposalSent = state.proposalSentDogId == dog.dogId,
                isSendingProposal = state.isSendingProposal,
                chatRoomId = state.acceptedChatRooms[dog.dogId],
                onDismiss = { viewModel.dismissDogProfile() },
                onPropose = { viewModel.sendProposal(dog.walkRecordId, dog.dogId) },
                onFeedback = { feedback -> viewModel.updateFeedback(dog.dogId, feedback) },
                onStartChat = { chatRoomId -> onNavigateToChat(chatRoomId) },
            )
        }

        // ── 채팅 메시지 배너 알림 (상단 슬라이드) ──────────────────────
        AnimatedVisibility(
            visible = state.chatBanner != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = androidx.compose.ui.Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp),
        ) {
            state.chatBanner?.let { banner ->
                ChatBannerCard(
                    banner = banner,
                    onDismiss = { viewModel.dismissChatBanner() },
                    onClick = {
                        viewModel.dismissChatBanner()
                        onNavigateToChat(banner.chatRoomId)
                    },
                )
            }
        }

        // ── 9. 받은 산책 제안 다이얼로그 ────────────────────────────────
        state.pendingProposals.firstOrNull()?.let { proposal ->
            ProposalIncomingDialog(
                proposal = proposal,
                onAccept = { viewModel.acceptProposal(proposal) },
                onReject = { viewModel.rejectProposal(proposal) }
            )
        }

        // ── 10. 제안자 — 수락 알림 다이얼로그 (polling) ─────────────────
        state.acceptedProposals.firstOrNull()?.let { accepted ->
            ProposalAcceptedDialog(
                accepted = accepted,
                onDismiss = { viewModel.dismissAcceptedProposal(accepted.proposalId) },
                onStartChat = { chatRoomId ->
                    viewModel.dismissAcceptedProposal(accepted.proposalId)
                    onNavigateToChat(chatRoomId)
                },
            )
        }

        // ── 10a. 제안자 — 거절 알림 다이얼로그 (polling) ─────────────────
        state.rejectedProposals.firstOrNull()?.let { rejected ->
            ProposalRejectedDialog(
                rejected = rejected,
                onDismiss = { viewModel.dismissRejectedProposal(rejected.proposalId) }
            )
        }

        // ── 10b. 수락자 — 수락 완료 확인 모달 (optimistic) ───────────────
        if (state.showAcceptedByMeDialog) {
            ProposalAcceptedByMeDialog(
                onDismiss = { viewModel.dismissAcceptedByMe() },
                chatRoomId = state.acceptedByMeChatRoomId,
                onStartChat = { chatRoomId ->
                    viewModel.dismissAcceptedByMe()
                    onNavigateToChat(chatRoomId)
                },
            )
        }

        // ── 10c. 거절자 — 거절 완료 확인 모달 (optimistic) ───────────────
        if (state.showRejectedByMeDialog) {
            ProposalRejectedByMeDialog(
                onDismiss = { viewModel.dismissRejectedByMe() }
            )
        }

        // ── 11. 비선호 강아지 경고 다이얼로그 (S14P21E108-175) ──────────
        state.warningDog?.let { dog ->
            DogWarningDialog(
                dog = dog,
                onDismiss = { viewModel.dismissWarning() },
            )
        }

        // ── 12. 위험장소 근접 경고 다이얼로그 (S14P21E108-275) ──────────
        // 큐 방식: 첫 번째 것 표시, 닫으면 다음 것 표시
        state.warningRiskZoneQueue.firstOrNull()?.let { zone ->
            RiskZoneWarningDialog(
                zone = zone,
                currentLatitude = currentPosition?.latitude,
                currentLongitude = currentPosition?.longitude,
                onDismiss = { viewModel.dismissRiskZoneWarning() },
            )
        }

        // ── 6. 위험 구역 신고 모달 ──────────────────────────────────────
        if (state.isDangerReportDialogOpen) {
            DangerReportModal(
                selectedReason = state.selectedDangerReason,
                customReason = state.customDangerReason,
                isLoading = state.isLoading,
                error = state.error,
                onReasonSelect = { viewModel.selectDangerReason(it) },
                onCustomReasonChange = { viewModel.updateCustomDangerReason(it) },
                onSubmit = { viewModel.submitDangerReport() },
                onDismiss = { viewModel.closeDangerReportDialog() }
            )
        }

        // ── 7. 산책 요약 바텀시트 ────────────────────────────────────
        if (state.isWalkSummaryVisible) {
            WalkSummarySheet(
                state = state,
                onRating = { viewModel.setWalkRating(it) },
                onNavigateToRecord = {
                    val walkId = state.summaryWalkId
                    viewModel.dismissWalkSummary()
                    if (walkId != null) {
                        onNavigateToWalkDetail(walkId)
                    } else {
                        onNavigateToRecord()
                    }
                },
                onNavigateToHome = {
                    viewModel.dismissWalkSummary()
                    onNavigateToHome()
                },
                onDismiss = { viewModel.dismissWalkSummary() }
            )
        }

        // ── 8. 배지 획득 팝업 ────────────────────────────────────────
        if (state.newBadges.isNotEmpty()) {
            BadgeEarnedDialog(
                badges = state.newBadges,
                onDismiss = { viewModel.dismissNewBadges() }
            )
        }
    }
    }
}

// ── 산책 요약 바텀시트 ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalkSummarySheet(
    state: WalkState,
    onRating: (Int) -> Unit,
    onNavigateToRecord: () -> Unit,
    onNavigateToHome: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val minutes = state.summaryElapsedSeconds / 60
    val seconds = state.summaryElapsedSeconds % 60
    val distanceKm = state.summaryDistanceMeters / 1000.0
    val calories = (distanceKm * 65).toInt()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 제목
            Text(
                text = "오늘의 산책 완료!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "우리 강아지와 함께한 즐거운 시간",
                fontSize = 14.sp,
                color = Color.Gray,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 통계 카드 3개
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatCard(
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = Color(0xFFE8873A), modifier = Modifier.size(28.dp)) },
                    label = "거리",
                    value = "%.1f".format(distanceKm),
                    unit = "km",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    icon = { Icon(Icons.Filled.Schedule, null, tint = Color(0xFFE8873A), modifier = Modifier.size(28.dp)) },
                    label = "시간",
                    value = "${minutes}분 ${seconds}",
                    unit = "초",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    icon = { Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFE8873A), modifier = Modifier.size(28.dp)) },
                    label = "칼로리",
                    value = "$calories",
                    unit = "kcal",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 산책 코스
            if (state.summaryRouteName.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("산책 코스  ", fontSize = 14.sp, color = Color.Gray)
                    Text(state.summaryRouteName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 별점
            Text(
                text = "만족하셨나요?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextMain,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.align(Alignment.Start),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < state.summaryRating) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "${index + 1}점",
                        tint = if (index < state.summaryRating) Color(0xFFFFA726) else Color(0xFFCCCCCC),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onRating(index + 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 일기 보러가기 버튼
            Button(
                onClick = onNavigateToRecord,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PointGreen)
            ) {
                Icon(Icons.Filled.Pets, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("일기 보러가기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 홈으로 돌아가기 버튼
            OutlinedButton(
                onClick = onNavigateToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain)
            ) {
                Text("홈으로 돌아가기", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Text(unit, fontSize = 12.sp, color = TextMain, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

// ── 위험 구역 선택 모드: 상단 안내 배너 ─────────────────────────────────────
@Composable
private fun DangerZoneSelectionBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B35)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = "위험 구역을 선택해주세요",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ── 발자국 도장 프롬프트 배너 ──────────────────────────────────────────────────
@Composable
private fun FootprintStampBanner(
    placeName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PointGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dog_footprint),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = placeName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "닫기",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── 산책 중 통계 패널 (시간 / 거리 / 칼로리 + 버튼) ─────────────────────────
@Composable
private fun WalkingStatsPanel(
    elapsedSeconds: Int,
    distanceMeters: Double,
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onEndWalk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    val distanceKm = distanceMeters / 1000.0
    val distanceText = "%.2fkm".format(distanceKm)

    // 칼로리 추정: 체중 미입력 시 평균 65kcal/km
    val calories = (distanceKm * 65).toInt()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 통계 행
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WalkStatItem(label = "시간", value = timeText)
                WalkStatItem(label = "거리", value = distanceText)
                WalkStatItem(label = "칼로리", value = "$calories")
            }

            // 버튼 행
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 일시정지 / 재개
                Button(
                    onClick = onPauseResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PointGreen)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPaused) "재개" else "일시정지",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 산책 종료
                Button(
                    onClick = onEndWalk,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF0F0F0),
                        contentColor = TextMain
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "산책 종료",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WalkStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
    }
}

// ── 위험 구역 선택 모드: 하단 "선택하기" 버튼 패널 ──────────────────────────
@Composable
private fun DangerZoneBottomPanel(
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSelectClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35))
    ) {
//        Image(
//            painter = painterResource(id = R.drawable.danger_flag),
//            contentDescription = null,
//            modifier = Modifier.size(22.dp)
//        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "이 위치로 신고하기",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ── 카카오맵 뷰 ───────────────────────────────────────────────────────────────
@Composable
private fun KakaoMapView(
    modifier: Modifier = Modifier,
    onMapReady: (KakaoMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    // onMapReady 완료 여부 (false이면 resume/pause/finish 호출 불가)
    val mapStarted = remember { mutableStateOf(false) }
    // onDispose가 onMapReady보다 먼저 불린 경우 → onMapReady에서 finish() 위임
    val finishPending = remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (!mapStarted.value) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> runCatching { mapView.resume() }
                Lifecycle.Event.ON_PAUSE  -> runCatching { mapView.pause() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (mapStarted.value) {
                // onMapReady 완료 후 정상 경로: 바로 finish
                runCatching { mapView.finish() }
            } else {
                // start()가 아직 완료 전: onMapReady 콜백에서 finish 처리
                // (finish()를 미완료 상태의 SDK에 호출하면 내부 상태 오염)
                finishPending.value = true
            }
        }
    }

    AndroidView(
        factory = { _ ->
            mapView.apply {
                runCatching {
                    start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {}
                            override fun onMapError(error: Exception) {
                                android.util.Log.e("KakaoMap", "onMapError: ${error.message}", error)
                                finishPending.value = false
                            }
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(kakaoMap: KakaoMap) {
                                android.util.Log.d("KakaoMap", "onMapReady 성공!")
                                mapStarted.value = true
                                if (finishPending.value) {
                                    // 이미 dispose됨 → resume 없이 바로 finish
                                    runCatching { mapView.finish() }
                                    return
                                }
                                // onMapReady가 ON_RESUME 이후에 도착한 경우(워치에서 산책 시작 등
                                // 네비게이션으로 화면에 진입할 때)에도 지도가 정상 표시되도록 resume() 호출
                                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                    runCatching { mapView.resume() }
                                }
                                onMapReady(kakaoMap)
                            }
                        }
                    )
                }.onFailure { e ->
                    android.util.Log.e("KakaoMap", "MapView.start() 실패: ${e.message}", e)
                }
            }
        },
        modifier = modifier
    )
}

// ── 위험 구역 깃발 마커 추가 ──────────────────────────────────────────────────
private fun addDangerZoneMarker(
    context: android.content.Context,
    kakaoMap: KakaoMap,
    zone: DangerZone
): Label? {
    val position = LatLng.from(zone.location.latitude, zone.location.longitude)
    val source = android.graphics.BitmapFactory.decodeResource(
        context.resources, R.drawable.danger_flag
    )
    val targetWidth = 60
    val targetHeight = (targetWidth * source.height.toFloat() / source.width).toInt()
    val scaled = source.scale(targetWidth, targetHeight)
    val style = LabelStyle.from(scaled).setAnchorPoint(0.5f, 1.0f)   // 하단 중앙을 좌표에 맞춤
    val styles = LabelStyles.from(style)
    val options = LabelOptions.from(position).setStyles(styles)
    return kakaoMap.labelManager?.layer?.addLabel(options)
}

// ── 장소 마커 추가 (강아지 집 아이콘) ─────────────────────────────────────────
private fun addPlaceMarker(
    context: android.content.Context,
    kakaoMap: KakaoMap,
    place: com.frontend.domain.model.Place
): Label? {
    val position = LatLng.from(place.latitude, place.longitude)

    val source = android.graphics.BitmapFactory.decodeResource(
        context.resources, R.drawable.place_mark
    )
    val targetSize = 80
    val aspectRatio = source.width.toFloat() / source.height.toFloat()
    val targetWidth = (targetSize * aspectRatio).toInt()
    val scaled = android.graphics.Bitmap.createScaledBitmap(source, targetWidth, targetSize, true)

    val style = LabelStyle.from(scaled).setAnchorPoint(0.5f, 1.0f)  // 하단 중앙을 좌표에 맞춤
    val styles = LabelStyles.from(style)
    val options = LabelOptions.from(position).setStyles(styles).setTag(place.id)
    return kakaoMap.labelManager?.layer?.addLabel(options)
}

// ── 발자국 마커 추가 (발자국 찍은 장소는 dog_footprint 아이콘 사용) ──────────────
private fun addFootprintMarker(
    context: android.content.Context,
    kakaoMap: KakaoMap,
    place: com.frontend.domain.model.Place
): Label? {
    val position = LatLng.from(place.latitude, place.longitude)

    val source = android.graphics.BitmapFactory.decodeResource(
        context.resources, R.drawable.dog_footprint
    )
    val targetSize = 96  // place_mark(80) 대비 1.2배
    val aspectRatio = source.width.toFloat() / source.height.toFloat()
    val targetWidth = (targetSize * aspectRatio).toInt()
    val scaled = android.graphics.Bitmap.createScaledBitmap(source, targetWidth, targetSize, true)

    val style = LabelStyle.from(scaled).setAnchorPoint(0.5f, 1.0f)
    val styles = LabelStyles.from(style)
    val options = LabelOptions.from(position).setStyles(styles).setTag(place.id)
    return kakaoMap.labelManager?.layer?.addLabel(options)
}

// ── FOV cone 업데이트 (Polygon 부채꼴) ───────────────────────────────────────
private fun updateFovCone(
    kakaoMap: KakaoMap,
    center: LatLng,
    azimuthDeg: Float,
    existingPolygon: Polygon?,
    radiusMeters: Double = 80.0,
    fovDeg: Float = 60f
): Polygon? {
    val points = calculateSectorPoints(center, azimuthDeg, radiusMeters, fovDeg)
    val mapPoints = MapPoints.fromLatLng(points)

    // 기존 polygon이 있으면 좌표만 갱신 (재생성 없이 → 깜빡임 완전 제거)
    if (existingPolygon != null) {
        existingPolygon.changeMapPoints(listOf(mapPoints))
        return existingPolygon
    }

    // 첫 생성
    val style = PolygonStyle.from(
        android.graphics.Color.argb(70, 167, 206, 146),  // 반투명 #A7CE92
        0f,                                               // 테두리 없음
        android.graphics.Color.TRANSPARENT
    )
    val options = PolygonOptions.from(mapPoints, style)
    return kakaoMap.shapeManager?.layer?.addPolygon(options)
}

// ── 부채꼴 꼭짓점 계산 (지구 구면 기반 LatLng 오프셋) ─────────────────────────
private fun calculateSectorPoints(
    center: LatLng,
    azimuthDeg: Float,
    radiusMeters: Double,
    fovDeg: Float,
    steps: Int = 20
): List<LatLng> {
    val earthRadius = 6371000.0
    val centerLatRad = Math.toRadians(center.latitude)
    val points = mutableListOf<LatLng>()

    // 중심점 (부채꼴 꼭짓점)
    points.add(center)

    // 호 위의 점들
    val startAngle = azimuthDeg - fovDeg / 2f
    val endAngle = azimuthDeg + fovDeg / 2f
    for (i in 0..steps) {
        val bearing = Math.toRadians((startAngle + (endAngle - startAngle) * i / steps).toDouble())
        val deltaLat = radiusMeters * Math.cos(bearing) / earthRadius * (180.0 / Math.PI)
        val deltaLng = radiusMeters * Math.sin(bearing) / (earthRadius * Math.cos(centerLatRad)) * (180.0 / Math.PI)
        points.add(LatLng.from(center.latitude + deltaLat, center.longitude + deltaLng))
    }

    return points
}

// ── 비트맵 회전 ───────────────────────────────────────────────────────────────
private fun rotateBitmap(source: android.graphics.Bitmap, degrees: Float): android.graphics.Bitmap {
    val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
    return android.graphics.Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

// ── 주변 강아지 마커 추가 ─────────────────────────────────────────────────────
private val nearbyDogDrawables = listOf(R.drawable.husky, R.drawable.poodle, R.drawable.french)

private suspend fun addNearbyDogMarker(
    context: android.content.Context,
    kakaoMap: KakaoMap,
    dog: com.frontend.domain.model.NearbyDogResponse,
): Label? {
    val position = LatLng.from(dog.latitude, dog.longitude)
    val markerSize = 80
    // avoidAlertCandidate 기반으로 비선호 강아지 판정 (기존 feedback 비교 대신)
    val isDisliked = dog.avoidAlertCandidate

    val circleBitmap = if (!dog.profileImageUrl.isNullOrBlank()) {
        try {
            val loader = coil.ImageLoader(context)
            val request = coil.request.ImageRequest.Builder(context)
                .data(dog.profileImageUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            val src = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (src != null) createCircularMarkerBitmap(src, markerSize)
            else fallbackMarkerBitmap(context, dog.dogId, markerSize)
        } catch (e: Exception) {
            fallbackMarkerBitmap(context, dog.dogId, markerSize)
        }
    } else {
        fallbackMarkerBitmap(context, dog.dogId, markerSize)
    }

    // 비선호 강아지는 빨간 테두리 + 외곽 글로우 효과 적용
    val finalBitmap = if (isDisliked) addDislikedRing(circleBitmap) else circleBitmap

    val style = LabelStyle.from(finalBitmap).setAnchorPoint(0.5f, 0.5f)
    val styles = LabelStyles.from(style)
    val options = LabelOptions.from(position).setStyles(styles).setTag(dog)
    return kakaoMap.labelManager?.layer?.addLabel(options)
}

/**
 * pulse 애니메이션용 비선호 강아지 마커 비트맵 생성
 * - 글로우 강도가 동적으로 변하는 효과
 */
private fun createPulsingAvoidMarkerBitmap(
    context: android.content.Context,
    dog: NearbyDogResponse,
    size: Int,
): android.graphics.Bitmap {
    // 기본 원형 마커 생성
    val circleBitmap = if (!dog.profileImageUrl.isNullOrBlank()) {
        try {
            // 이미 로드된 이미지가 있다면 사용 (여기서는 fallback 사용)
            fallbackMarkerBitmap(context, dog.dogId, size)
        } catch (e: Exception) {
            fallbackMarkerBitmap(context, dog.dogId, size)
        }
    } else {
        fallbackMarkerBitmap(context, dog.dogId, size)
    }

    // 강조된 글로우 효과 적용
    return addPulsingDislikedRing(circleBitmap, size)
}

/**
 * pulse 애니메이션용 강조된 글로우 효과
 */
private fun addPulsingDislikedRing(src: android.graphics.Bitmap, targetSize: Int): android.graphics.Bitmap {
    val glowPadding = 24   // 더 큰 글로우 여백
    val borderWidth = 6    // 더 두꺼운 테두리
    val total = targetSize + glowPadding * 2
    val output = android.graphics.Bitmap.createBitmap(total, total, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)

    // 1) 반투명 빨간 글로우 원 (더 진하게)
    val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(100, 255, 60, 60)
    }
    canvas.drawCircle(total / 2f, total / 2f, total / 2f, glowPaint)

    // 2) 불투명 빨간 테두리 원
    val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(240, 255, 60, 60)
    }
    val scaled = android.graphics.Bitmap.createScaledBitmap(src, targetSize, targetSize, true)
    val radius = targetSize / 2f + borderWidth
    canvas.drawCircle(total / 2f, total / 2f, radius, borderPaint)

    // 3) 원본 이미지 중앙에 합성
    val imgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    canvas.drawBitmap(scaled, glowPadding.toFloat(), glowPadding.toFloat(), imgPaint)

    return output
}

/**
 * 비선호 강아지 마커 — 빨간 반투명 외곽 원(글로우) + 빨간 테두리를 덧그림
 */
private fun addDislikedRing(src: android.graphics.Bitmap): android.graphics.Bitmap {
    val glowPadding = 18   // 외곽 글로우 여백
    val borderWidth = 5    // 빨간 테두리 두께
    val total = src.width + glowPadding * 2
    val output = android.graphics.Bitmap.createBitmap(total, total, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)

    // 1) 반투명 빨간 글로우 원
    val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(80, 255, 80, 80)
    }
    canvas.drawCircle(total / 2f, total / 2f, total / 2f, glowPaint)

    // 2) 불투명 빨간 테두리 원
    val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(220, 255, 80, 80)
    }
    val radius = src.width / 2f + borderWidth
    canvas.drawCircle(total / 2f, total / 2f, radius, borderPaint)

    // 3) 원본 이미지 중앙에 합성
    val imgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    canvas.drawBitmap(src, glowPadding.toFloat(), glowPadding.toFloat(), imgPaint)

    return output
}

private fun fallbackMarkerBitmap(
    context: android.content.Context,
    dogId: Long,
    size: Int,
): android.graphics.Bitmap {
    val drawableRes = nearbyDogDrawables[dogId.toInt() % nearbyDogDrawables.size]
    val source = android.graphics.BitmapFactory.decodeResource(context.resources, drawableRes)
    val targetWidth = (size * source.width.toFloat() / source.height).toInt()
    return android.graphics.Bitmap.createScaledBitmap(source, targetWidth, size, true)
}

private fun createCircularMarkerBitmap(source: android.graphics.Bitmap, size: Int): android.graphics.Bitmap {
    val output = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val scaled = android.graphics.Bitmap.createScaledBitmap(source, size, size, true)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaled, 0f, 0f, paint)
    return output
}

// ── 강아지 마커용 비트맵 생성 ─────────────────────────────────────────────────
private fun createDogMarkerBitmap(context: android.content.Context): android.graphics.Bitmap {
    val targetHeight = 80  // Kakao Map은 픽셀 그대로 렌더링 → density 곱하지 않음
    val source = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.normal_face)
    val aspectRatio = source.width.toFloat() / source.height.toFloat()
    val targetWidth = (targetHeight * aspectRatio).toInt()
    return source.scale(targetWidth, targetHeight)
}

// ── 채팅 메시지 배너 카드 ──────────────────────────────────────────────────────
@Composable
private fun ChatBannerCard(
    banner: com.frontend.domain.model.ChatBannerNotification,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    LaunchedEffect(banner) {
        kotlinx.coroutines.delay(3_500)
        onDismiss()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            coil.compose.AsyncImage(
                model = banner.senderImageUrl.takeIf { !it.isNullOrBlank() },
                contentDescription = banner.senderName,
                placeholder = painterResource(R.drawable.husky),
                error = painterResource(R.drawable.husky),
                fallback = painterResource(R.drawable.husky),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = banner.senderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black,
                )
                Text(
                    text = banner.messagePreview,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            androidx.compose.material3.IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "닫기",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ── 배지 획득 팝업 ───────────────────────────────────────────────────────────
@Composable
private fun BadgeEarnedDialog(
    badges: List<com.frontend.domain.model.NewBadgeInfo>,
    onDismiss: () -> Unit
) {
    val badge = badges.first()

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "배지를 획득했어요!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(Modifier.height(20.dp))

                val badgeDrawable = when (badge.badgeId) {
                    1L -> R.drawable.badge1
                    2L -> R.drawable.badge2
                    3L -> R.drawable.badge3
                    4L -> R.drawable.badge4
                    5L -> R.drawable.badge5
                    6L -> R.drawable.badge6
                    7L -> R.drawable.badge7
                    8L -> R.drawable.badge8
                    9L -> R.drawable.badge9
                    else -> R.drawable.badge1
                }

                Image(
                    painter = painterResource(badgeDrawable),
                    contentDescription = badge.badgeName,
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    badge.badgeName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    badge.description,
                    fontSize = 14.sp,
                    color = com.frontend.ui.theme.TextGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PointGreen)
                ) {
                    Text(
                        "확인했어요!",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── 발자국 찍기 오버레이 ──────────────────────────────────────────────────────
@Composable
private fun FootprintStampOverlay(
    stamped: Boolean,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!stamped) {
                Text(
                    text = "터치하세요",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .then(
                        if (!stamped) Modifier.clickable(
                            onClick = onTap,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Pets,
                    contentDescription = "발자국",
                    modifier = Modifier.size(90.dp),
                    tint = if (stamped) PointGreen else PointGreen.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (stamped) "발자국을 남겼어요!" else "발자국을 남겨보세요!",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
