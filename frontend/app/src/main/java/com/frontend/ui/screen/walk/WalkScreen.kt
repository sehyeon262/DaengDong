package com.frontend.ui.screen.walk

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.frontend.ui.component.MapOverlayButton
import com.frontend.ui.screen.walk.components.WalkFilterBottomSheet
import com.frontend.ui.screen.walk.components.WalkRouteCard
import com.frontend.ui.screen.walk.components.WalkSearchBar
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextMain
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory

@Composable
fun WalkScreen(
    viewModel: WalkViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val routes = viewModel.routes
    val context = LocalContext.current

    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }

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

    // 외부에서 selectedRouteIndex 변경 시 페이저 스크롤
    LaunchedEffect(state.selectedRouteIndex) {
        if (pagerState.currentPage != state.selectedRouteIndex) {
            pagerState.animateScrollToPage(state.selectedRouteIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. 카카오맵 ─────────────────────────────────────────────────
        KakaoMapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map -> kakaoMap = map }
        )

        // ── 2. 지도 위 강아지 캐릭터 + 시야 원뿔 ────────────────────
        MapCharacter(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
        )

        // ── 3. 전체 오버레이 레이아웃 (검색바 + 하단 패널) ───────────
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 검색바
            WalkSearchBar(
                onBackClick = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 하단 패널
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
                    contentPadding = PaddingValues(start = 20.dp, end = 160.dp),
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
                        .height(54.dp)
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

        // ── 4. 우측 플로팅 버튼 (캐릭터, 현재위치, 필터) ──────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapOverlayButton(
                painter = painterResource(id = R.drawable.alert),
                contentDescription = "캐릭터",
                onClick = {}
            )
            MapOverlayButton(
                icon = Icons.Filled.GpsFixed,
                contentDescription = "현재 위치",
                onClick = {
                    moveToCurrentLocation(context, kakaoMap)
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

// ── 현재 위치로 카메라 이동 ────────────────────────────────────────────────────
private fun moveToCurrentLocation(context: android.content.Context, kakaoMap: KakaoMap?) {
    if (kakaoMap == null) return
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) return

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        location?.let {
            val position = LatLng.from(it.latitude, it.longitude)
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(position, 15))
        }
    }
}

// ── 지도 위 강아지 캐릭터 + 시야 원뿔 ────────────────────────────────────────
@Composable
private fun MapCharacter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        // 시야 원뿔 (Canvas)
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .width(120.dp)
                .height(130.dp)
                .align(Alignment.BottomCenter)
        ) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(0f, size.height)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(
                path = path,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        PointGreen.copy(alpha = 0.45f),
                        PointGreen.copy(alpha = 0.15f)
                    )
                )
            )
        }

        // 강아지 캐릭터 이미지
        Image(
            painter = painterResource(id = R.drawable.normal),
            contentDescription = "강아지 캐릭터",
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Fit
        )
    }
}
