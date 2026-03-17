package com.frontend.ui.screen.walk

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frontend.R
import com.frontend.ui.component.MapOverlayButton
import com.frontend.ui.screen.walk.components.WalkRouteCard
import com.frontend.ui.screen.walk.components.WalkSearchBar
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextMain

@Composable
fun WalkScreen(
    viewModel: WalkViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val routes = viewModel.routes

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

        // ── 1. 지도 배경 ──────────────────────────────────────────────
        MapBackground(modifier = Modifier.fillMaxSize())

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
                onClick = {}
            )
            MapOverlayButton(
                icon = Icons.Filled.FilterAlt,
                contentDescription = "필터",
                onClick = {}
            )
        }
    }
}

// ── 지도 플레이스홀더 ─────────────────────────────────────────────────────────
@Composable
private fun MapBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // 배경 (연한 베이지/회색 - 지도 색상)
        drawRect(color = Color(0xFFEEECE4))

        // 도로 (수평)
        val roadColor = Color(0xFFFFFFFF)
        val roadWidth = 28.dp.toPx()
        drawRect(
            color = roadColor,
            topLeft = Offset(0f, size.height * 0.35f - roadWidth / 2),
            size = androidx.compose.ui.geometry.Size(size.width, roadWidth)
        )
        drawRect(
            color = roadColor,
            topLeft = Offset(0f, size.height * 0.65f - roadWidth / 2),
            size = androidx.compose.ui.geometry.Size(size.width, roadWidth)
        )

        // 도로 (수직)
        drawRect(
            color = roadColor,
            topLeft = Offset(size.width * 0.28f - roadWidth / 2, 0f),
            size = androidx.compose.ui.geometry.Size(roadWidth, size.height)
        )
        drawRect(
            color = roadColor,
            topLeft = Offset(size.width * 0.68f - roadWidth / 2, 0f),
            size = androidx.compose.ui.geometry.Size(roadWidth, size.height)
        )

        // 건물 블록
        val blockColor = Color(0xFFDDDACF)
        drawRoundRect(
            color = blockColor,
            topLeft = Offset(size.width * 0.05f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.20f, size.height * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
        )
        drawRoundRect(
            color = blockColor,
            topLeft = Offset(size.width * 0.32f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.32f, size.height * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
        )
        drawRoundRect(
            color = blockColor,
            topLeft = Offset(size.width * 0.72f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.22f, size.height * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
        )
        drawRoundRect(
            color = blockColor,
            topLeft = Offset(size.width * 0.05f, size.height * 0.38f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.20f, size.height * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
        )
        drawRoundRect(
            color = blockColor,
            topLeft = Offset(size.width * 0.32f, size.height * 0.38f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.32f, size.height * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
        )
        drawRoundRect(
            color = blockColor,
            topLeft = Offset(size.width * 0.72f, size.height * 0.38f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.22f, size.height * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
        )
        drawRoundRect(
            color = blockColor,
            topLeft = Offset(size.width * 0.05f, size.height * 0.68f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.20f, size.height * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
        )
        drawRoundRect(
            color = blockColor,
            topLeft = Offset(size.width * 0.32f, size.height * 0.68f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.32f, size.height * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
        )
        drawRoundRect(
            color = blockColor,
            topLeft = Offset(size.width * 0.72f, size.height * 0.68f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.22f, size.height * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
        )
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
        Canvas(
            modifier = Modifier
                .width(120.dp)
                .height(130.dp)
                .align(Alignment.BottomCenter)
        ) {
            val path = Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(0f, size.height)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
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
