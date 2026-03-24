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
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.frontend.domain.model.NearbyDogResponse
import com.frontend.ui.component.MapOverlayButton
import com.frontend.ui.screen.walk.components.DangerReportModal
import com.frontend.ui.screen.walk.components.DogWarningDialog
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
import com.kakao.vectormap.camera.CameraPosition
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
    onNavigateToRecord: () -> Unit = {},
    onNavigateToWalkDetail: (Long) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
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
    var fovOverlay by remember { mutableStateOf<Polygon?>(null) }
    // GPS 버튼으로 트래킹 활성화 여부 (사용자가 지도를 드래그하면 자동 해제)
    var isTrackingActive by remember { mutableStateOf(false) }
    // GPS 버튼 모드: 0=꺼짐, 1=위치 추적, 2=방향 추적(heading up)
    var gpsMode by remember { mutableStateOf(0) }

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
        if (granted) viewModel.retryPhotoObserverIfWalking()
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
            // 미디어 권한 요청
            val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            val hasMediaPermission = ActivityCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED
            if (!hasMediaPermission) {
                mediaPermissionLauncher.launch(mediaPermission)
            }

            // 알림 권한 요청 (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (viewModel.needsNotificationPermission()) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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

    // 지도 준비 완료 시 위치 트래킹 시작
    LaunchedEffect(kakaoMap) {
        if (kakaoMap == null) return@LaunchedEffect
        val hasPermission = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.startLocationTracking()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 위치 변경 시 마커 + FOV cone 갱신
    LaunchedEffect(currentPosition, kakaoMap) {
        val pos = currentPosition ?: return@LaunchedEffect
        val map = kakaoMap ?: return@LaunchedEffect

        if (currentLocationLabel == null) {
            // 최초: 카메라 이동 + 마커 생성 + TrackingManager 시작
            // 최초: 카메라 이동 + 마커 생성 (트래킹은 GPS 버튼으로만 활성화)
            map.moveCamera(CameraUpdateFactory.newCenterPosition(pos, 15))
            val bitmap = rotateBitmap(createDogMarkerBitmap(context), azimuth)
            val styles = LabelStyles.from(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f))
            val label = map.labelManager?.layer?.addLabel(LabelOptions.from(pos).setStyles(styles))
            currentLocationLabel = label
            if (label != null) {
                map.trackingManager?.startTracking(label)
            }

            // 최초 위치 수신 시 추천 경로 로드
            viewModel.loadRecommendedRoutes(pos.latitude, pos.longitude)
        } else {
            // 이후: moveTo()로 이동 (마커 사라짐 없이)
            currentLocationLabel?.moveTo(pos)
            // 방향 추적 모드: 위치 변경 시 카메라도 이동 (rotation 유지)
            if (gpsMode == 2) {
                map.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.from(pos.latitude, pos.longitude, 15, 0.0, azimuth.toDouble(), 0.0)
                    )
                )
            }
        }

        // FOV cone 갱신
        fovOverlay = updateFovCone(map, pos, azimuth, fovOverlay)
    }

    // azimuth 변경 시 강아지 마커 회전 + FOV cone 업데이트
    LaunchedEffect(azimuth) {
        val pos = currentPosition ?: return@LaunchedEffect
        val map = kakaoMap ?: return@LaunchedEffect

        // changeStyles()로 비트맵만 교체 (마커 사라짐 없이)
        val bitmap = rotateBitmap(createDogMarkerBitmap(context), azimuth)
        val styles = LabelStyles.from(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f))
        currentLocationLabel?.changeStyles(styles)

        // FOV cone 업데이트
        fovOverlay = updateFovCone(map, pos, azimuth, fovOverlay)

        // 방향 추적 모드: azimuth 변경 시 카메라 회전
        if (gpsMode == 2) {
            map.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.from(pos.latitude, pos.longitude, 15, 0.0, azimuth.toDouble(), 0.0)
                )
            )
        }
    }

    // 외부에서 selectedRouteIndex 변경 시 페이저 스크롤
    LaunchedEffect(state.selectedRouteIndex) {
        if (pagerState.currentPage != state.selectedRouteIndex) {
            pagerState.animateScrollToPage(state.selectedRouteIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 사용자가 지도를 드래그하면 트래킹 중단 (이벤트는 소비하지 않아 지도에 그대로 전달)
            .pointerInput(gpsMode) {
                if (gpsMode == 0) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.position != it.previousPosition }) {
                            kakaoMap?.trackingManager?.stopTracking()
                            isTrackingActive = false
                            gpsMode = 0
                            break
                        }
                    }
                }
            }
    ) {
    // 위험 구역 선택 모드 진입/종료 시 TrackingManager 제어
    // 선택 모드: tracking 중단 → 지도 드래그 위치 유지
    // 선택 모드 해제: tracking 재개 → 강아지 마커 다시 따라가기
    LaunchedEffect(state.isSelectingDangerZone, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (state.isSelectingDangerZone) {
            map.trackingManager?.stopTracking()
        } else {
            currentLocationLabel?.let { label ->
                map.trackingManager?.startTracking(label)
            }
        }
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

    // 새 위험 구역이 추가될 때마다 지도에 깃발 마커 그리기 (기존 마커 유지)
    LaunchedEffect(state.dangerZones, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        val newZones = state.dangerZones.drop(dangerZoneLabels.size)
        newZones.forEach { zone ->
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

        // 경로 표시 OFF이면 폴리라인 제거
        if (!state.showRecommendedRoute) {
            recommendedRoutePolyline?.let {
                map.shapeManager?.layer?.remove(it)
                recommendedRoutePolyline = null
            }
            return@LaunchedEffect
        }

        // 선택된 추천 경로 가져오기 (자유 산책이면 null)
        val selectedRoute = viewModel.getSelectedRecommendedRoute()

        // 추천 경로가 없으면 폴리라인 제거
        if (selectedRoute == null) {
            recommendedRoutePolyline?.let {
                map.shapeManager?.layer?.remove(it)
                recommendedRoutePolyline = null
            }
            return@LaunchedEffect
        }

        // 경로 포인트 가져오기 (actualPathPoints 우선, 없으면 polyline)
        val pathPoints = selectedRoute.getPathPoints()
        if (pathPoints.size < 2) {
            recommendedRoutePolyline?.let {
                map.shapeManager?.layer?.remove(it)
                recommendedRoutePolyline = null
            }
            return@LaunchedEffect
        }

        // LatLngPoint를 KakaoMap LatLng로 변환
        val latLngList = pathPoints.map { LatLng.from(it.latitude, it.longitude) }
        val mapPoints = MapPoints.fromLatLng(latLngList)

        // 기존 폴리라인 제거 후 새로 생성 (경로 전환 시 깔끔하게)
        recommendedRoutePolyline?.let {
            map.shapeManager?.layer?.remove(it)
        }

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
    LaunchedEffect(state.places, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        placeLabels.forEach { map.labelManager?.layer?.remove(it) }
        placeLabels.clear()
        if (state.places.isEmpty()) return@LaunchedEffect
        state.places.forEach { place ->
            val label = addPlaceMarker(context, map, place)
            if (label != null) placeLabels.add(label)
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
                        enabled = !state.isWalking,
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
                icon = if (gpsMode == 2) Icons.Filled.Navigation else Icons.Filled.GpsFixed,
                contentDescription = "현재 위치",
                onClick = {
                    when (gpsMode) {
                        0 -> {
                            // 1번 누름: 현재 위치로 카메라 이동 + 위치 추적 시작
                            currentLocationLabel?.let { label ->
                                kakaoMap?.trackingManager?.startTracking(label)
                                isTrackingActive = true
                                gpsMode = 1
                            }
                        }
                        1 -> {
                            // 2번 누름: 방향 추적 모드 (heading up)
                            kakaoMap?.trackingManager?.stopTracking()
                            isTrackingActive = false
                            gpsMode = 2
                            currentPosition?.let { pos ->
                                kakaoMap?.moveCamera(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.from(pos.latitude, pos.longitude, 15, 0.0, azimuth.toDouble(), 0.0)
                                    )
                                )
                            }
                        }
                        else -> {
                            // 3번 누름: 초기화 (North up)
                            gpsMode = 0
                            isTrackingActive = false
                        }
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
                onDismiss = { viewModel.dismissDogProfile() },
                onPropose = { viewModel.sendProposal(dog.walkRecordId, dog.dogId) },
                onFeedback = { feedback -> viewModel.updateFeedback(dog.dogId, feedback) },
            )
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
                onDismiss = { viewModel.dismissAcceptedProposal(accepted.proposalId) }
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
                onDismiss = { viewModel.dismissAcceptedByMe() }
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

    // MapView.start() 성공 여부 추적 (실패 시 resume/pause NPE 방지)
    var mapStarted by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (!mapStarted) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> runCatching { mapView.resume() }
                Lifecycle.Event.ON_PAUSE  -> runCatching { mapView.pause() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { mapView.finish() }
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
                            }
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(kakaoMap: KakaoMap) {
                                android.util.Log.d("KakaoMap", "onMapReady 성공!")
                                mapStarted = true
                                onMapReady(kakaoMap)
                            }
                        }
                    )
                }.onFailure { e ->
                    android.util.Log.e("KakaoMap", "MapView.start() 실패: ${e.message}", e)
                }
                // start() 이후 이미 RESUMED 상태이면 resume() 호출
                if (mapStarted && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    runCatching { resume() }
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

// ── 발자국 마커 추가 (place_mark에 초록 틴트 적용) ──────────────────────────
private fun addFootprintMarker(
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

    // 초록 틴트를 적용해 일반 장소 마커(파랑)와 구분
    val tinted = scaled.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(tinted)
    val paint = android.graphics.Paint()
    paint.colorFilter = android.graphics.PorterDuffColorFilter(
        android.graphics.Color.argb(180, 76, 175, 80),  // 반투명 녹색 #4CAF50
        android.graphics.PorterDuff.Mode.SRC_ATOP
    )
    canvas.drawBitmap(scaled, 0f, 0f, paint)

    val style = LabelStyle.from(tinted).setAnchorPoint(0.5f, 1.0f)
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
