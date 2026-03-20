package com.frontend.ui.screen.walk

import android.Manifest
import android.content.pm.PackageManager
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
import com.kakao.vectormap.shape.Polyline
import com.kakao.vectormap.shape.PolylineOptions
import com.kakao.vectormap.shape.PolylineStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkScreen(
    onNavigateToRecord: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    viewModel: WalkViewModel = hiltViewModel(),
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

    // 산책 경로 폴리라인
    val routePoints by viewModel.routePoints.collectAsState()
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }

    // 위험 구역 마커 목록 (강아지 마커와 분리 관리)
    val dangerZoneLabels = remember { mutableStateListOf<Label>() }

    // 장소 마커 목록 (PLACE 필터 on/off 시 추가/제거)
    val placeLabels = remember { mutableStateListOf<Label>() }

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

    // 산책 경로 폴리라인 실시간 업데이트
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
                    // TrackingManager 재활성화 (수동 이동 후 다시 마커 따라가기)
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
                activeFilters = state.pendingFilters,
                onFilterToggle = { viewModel.toggleFilter(it) },
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

        // ── 7. 산책 요약 바텀시트 ────────────────────────────────────
        if (state.isWalkSummaryVisible) {
            WalkSummarySheet(
                state = state,
                onRating = { viewModel.setWalkRating(it) },
                onNavigateToRecord = {
                    viewModel.dismissWalkSummary()
                    onNavigateToRecord()
                },
                onNavigateToHome = {
                    viewModel.dismissWalkSummary()
                    onNavigateToHome()
                },
                onDismiss = { viewModel.dismissWalkSummary() }
            )
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
