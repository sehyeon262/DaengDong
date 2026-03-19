package com.frontend.ui.screen.walk

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.frontend.ui.component.MapOverlayButton
import com.frontend.ui.screen.walk.components.DangerReportModal
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

@Composable
fun WalkScreen(
    viewModel: WalkViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val routes = viewModel.routes
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

    // 위험 구역 마커 목록 (강아지 마커와 분리 관리)
    val dangerZoneLabels = remember { mutableStateListOf<Label>() }

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
            // 최초: 카메라 이동 + 마커 생성 (트래킹은 GPS 버튼으로만 활성화)
            map.moveCamera(CameraUpdateFactory.newCenterPosition(pos, 15))
            val bitmap = rotateBitmap(createDogMarkerBitmap(context), azimuth)
            val styles = LabelStyles.from(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f))
            val label = map.labelManager?.layer?.addLabel(LabelOptions.from(pos).setStyles(styles))
            currentLocationLabel = label
            if (label != null) {
                map.trackingManager?.startTracking(label)
            }
        } else {
            // 이후: moveTo()로 이동 (마커 사라짐 없이)
            currentLocationLabel?.moveTo(pos)
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
            .pointerInput(isTrackingActive) {
                if (!isTrackingActive) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.position != it.previousPosition }) {
                            kakaoMap?.trackingManager?.stopTracking()
                            isTrackingActive = false
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

    // 새 위험 구역이 추가될 때마다 지도에 깃발 마커 그리기 (기존 마커 유지)
    LaunchedEffect(state.dangerZones, kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        val newZones = state.dangerZones.drop(dangerZoneLabels.size)
        newZones.forEach { zone ->
            val label = addDangerZoneMarker(context, map, zone)
            if (label != null) dangerZoneLabels.add(label)
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
                        onClick = {},
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
                    // TrackingManager 활성화: 마커를 따라 카메라 이동
                    currentLocationLabel?.let { label ->
                        kakaoMap?.trackingManager?.startTracking(label)
                        isTrackingActive = true
                    }
                }
            )
            MapOverlayButton(
                icon = Icons.Filled.FilterAlt,
                contentDescription = "필터",
                onClick = { viewModel.showFilter() }
            )
        }

        // ── 5. 필터 바텀시트 ────────────────────────────────────────────
        if (state.showFilterSheet) {
            WalkFilterBottomSheet(
                selectedFilter = state.selectedFilter,
                onFilterSelect = { viewModel.selectFilter(it) },
                onApply = { viewModel.applyFilter() },
                onDismiss = { viewModel.hideFilter() }
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    AndroidView(
        factory = { _ ->
            mapView.apply {
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
                            onMapReady(kakaoMap)
                        }
                    }
                )
                // start() 이후 이미 RESUMED 상태이면 resume() 호출
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    resume()
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
    val scaled = android.graphics.Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    val style = LabelStyle.from(scaled).setAnchorPoint(0.5f, 1.0f)   // 하단 중앙을 좌표에 맞춤
    val styles = LabelStyles.from(style)
    val options = LabelOptions.from(position).setStyles(styles)
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

// ── 강아지 마커용 비트맵 생성 ─────────────────────────────────────────────────
private fun createDogMarkerBitmap(context: android.content.Context): android.graphics.Bitmap {
    val targetHeight = 80  // Kakao Map은 픽셀 그대로 렌더링 → density 곱하지 않음
    val source = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.normal_face)
    val aspectRatio = source.width.toFloat() / source.height.toFloat()
    val targetWidth = (targetHeight * aspectRatio).toInt()
    return source.scale(targetWidth, targetHeight)
}
