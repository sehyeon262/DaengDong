package com.frontend.ui.screen.walk

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.painterResource
import com.frontend.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.frontend.domain.model.WalkDetailResponse
import com.frontend.ui.theme.Background
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import com.frontend.ui.theme.White
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkDetailScreen(
    navController: NavController,
    viewModel: WalkDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "산책 상세",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = TextMain
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = TextMain
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PointGreen
                    )
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error ?: "오류가 발생했습니다",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadWalkDetail() }) {
                            Text("다시 시도", color = PointGreen)
                        }
                    }
                }

                state.detail != null -> {
                    WalkDetailContent(
                        detail = state.detail!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun WalkDetailContent(
    detail: WalkDetailResponse,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 사진 캐러셀
        PhotoCarousel(
            photoUrls = detail.photoUrls,
            startTime = detail.startTime
        )

        // 산책 통계 카드
        WalkStatsCard(detail = detail)

        // 일기 카드
        DiaryCard(
            dogName = detail.dogName,
            diary = detail.diary
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PhotoCarousel(
    photoUrls: List<String>,
    startTime: String
) {
    val photos = photoUrls.ifEmpty { listOf("") } // 사진 없으면 placeholder 1장
    val pagerState = rememberPagerState(pageCount = { photos.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE0E0E0))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val url = photos[page]
            if (url.isNotBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = "산책 사진 ${page + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 사진 없을 때 플레이스홀더
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF0EEE8)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uD83D\uDC3E",
                        fontSize = 48.sp
                    )
                }
            }
        }

        // 하단 그라데이션 + 날짜/시간 오버레이
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Column {
                    Text(
                        text = formatDate(startTime),
                        fontSize = 11.sp,
                        color = White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = formatTime(startTime),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                }
            }
        }

        // 사진 인디케이터 (우측 상단)
        if (photos.size > 1 || photoUrls.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${photos.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = White
                )
            }
        }
    }
}

@Composable
private fun WalkStatsCard(detail: WalkDetailResponse) {
    val statIconBg = Color(0xFFFFFCEE)   // 배경보다 아주 살짝 진한 연한 노란색

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(White)
            .border(
                width = 1.dp,
                color = PointGreen.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // 경로 보기
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "경로 보기 >",
                fontSize = 13.sp,
                color = PointGreen,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 통계 3개 (사이에 구분선)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.Default.AccessTime,
                iconBgColor = statIconBg,
                iconTint = PointGreen,
                label = "산책 시간",
                value = "${detail.durationMinutes}분"
            )
            // 구분선
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(PointGreen.copy(alpha = 0.2f))
            )
            StatItem(
                icon = Icons.Default.LocationOn,
                iconBgColor = statIconBg,
                iconTint = PointGreen,
                label = "산책 거리",
                value = "%.1f km".format(detail.distanceKm)
            )
            // 구분선
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(PointGreen.copy(alpha = 0.2f))
            )
            StatItem(
                icon = Icons.Default.Whatshot,
                iconBgColor = statIconBg,
                iconTint = PointGreen,
                label = "칼로리",
                value = "%.0f kcal".format(detail.calories)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))   // 둥근 사각형
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            textAlign = TextAlign.Center
        )
    }
}

private val mansehFont = FontFamily(Font(R.font.yoonchildfundkoreamanseh))

// 받침 있으면 "이의", 없으면 "의"
private fun diaryTitle(name: String): String {
    if (name.isEmpty()) return "${name}의 일기"
    val lastChar = name.last()
    val code = lastChar.code
    return if (code in 0xAC00..0xD7A3 && (code - 0xAC00) % 28 != 0) {
        "${name}이의 일기"
    } else {
        "${name}의 일기"
    }
}

@Composable
private fun DiaryCard(
    dogName: String,
    diary: WalkDetailResponse.DiaryInfo?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(White)
            .border(
                width = 1.dp,
                color = PointGreen.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // 꾸미기 이미지들 (배경 레이어)

        // 오른쪽 상단 - 꽃
        Image(
            painter = painterResource(R.drawable.diary2),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp),
            alpha = 0.1f
        )
        // 왼쪽 하단 - 발바닥
        Image(
            painter = painterResource(R.drawable.diary3),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-6).dp, y = 10.dp),
            alpha = 0.1f
        )
        // 오른쪽 하단 - 강아지 얼굴
        Image(
            painter = painterResource(R.drawable.diary1),
            contentDescription = null,
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 14.dp, y = 18.dp),
            alpha = 0.12f
        )
        // 가운데 상단 오른쪽 - 구름
        Image(
            painter = painterResource(R.drawable.diary4),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.TopStart)
                .offset(x = (-8).dp, y = (-8).dp),
            alpha = 0.08f
        )

        // 텍스트 레이어 (앞)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp)
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 헤더
            Text(
                text = diaryTitle(dogName),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PointGreen,
                fontFamily = mansehFont
            )

            // 일기 내용
            when {
                diary == null -> {
                    Text(
                        text = "아직 일기가 없어요",
                        fontSize = 16.sp,
                        color = TextGray,
                        lineHeight = 34.sp,
                        fontFamily = mansehFont
                    )
                }

                diary.content == null -> {
                    DiaryLoadingIndicator()
                }

                else -> {
                    Text(
                        text = diary.content,
                        fontSize = 16.sp,
                        color = TextMain,
                        lineHeight = 34.sp,
                        fontFamily = mansehFont
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryLoadingIndicator() {
    val fullText = "일기를 쓰고 있어요"
    var charCount by remember { mutableIntStateOf(0) }
    var isAdding by remember { androidx.compose.runtime.mutableStateOf(true) }

    // 글자 하나씩 추가/삭제 타이핑 애니메이션
    LaunchedEffect(Unit) {
        while (true) {
            if (isAdding) {
                if (charCount < fullText.length) {
                    charCount++
                    kotlinx.coroutines.delay(120)
                } else {
                    kotlinx.coroutines.delay(600)
                    isAdding = false
                }
            } else {
                if (charCount > 0) {
                    charCount--
                    kotlinx.coroutines.delay(70)
                } else {
                    kotlinx.coroutines.delay(300)
                    isAdding = true
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 강아지 일기 쓰는 이미지
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.loading),
            contentDescription = "일기 생성 중",
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1.3f),
            contentScale = ContentScale.Fit
        )

        // 타이핑 텍스트
        Text(
            text = fullText.take(charCount) + "✏️",
            fontSize = 15.sp,
            color = PointGreen,
            fontFamily = mansehFont,
            fontWeight = FontWeight.Medium
        )
    }
}

// "2026-03-12T16:30:00" → "2026년 3월 12일"
private fun formatDate(dateTimeStr: String): String {
    return try {
        val date = dateTimeStr.substringBefore("T")
        val parts = date.split("-")
        "${parts[0]}년 ${parts[1].toInt()}월 ${parts[2].toInt()}일"
    } catch (e: Exception) {
        dateTimeStr
    }
}

// "2026-03-12T16:30:00" → "오후 4:30"
private fun formatTime(dateTimeStr: String): String {
    return try {
        val time = dateTimeStr.substringAfter("T")
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1]
        val amPm = if (hour < 12) "오전" else "오후"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$amPm $displayHour:$minute"
    } catch (e: Exception) {
        dateTimeStr
    }
}
