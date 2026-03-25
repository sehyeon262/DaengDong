package com.frontend.ui.screen.walk

import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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
                        viewModel = viewModel,
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
    viewModel: WalkDetailViewModel,
    modifier: Modifier = Modifier
) {
    var showViewer by remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // 갤러리 선택 런처 (PickMultipleVisualMedia: 권한 없이도 모든 사진 선택 가능)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val parts = uris.mapNotNull { uri ->
                val bytes = compressImage(context, uri) ?: return@mapNotNull null
                val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("files", "photo_${System.currentTimeMillis()}.jpg", requestBody)
            }
            if (parts.isNotEmpty()) viewModel.uploadPhotos(parts)
        }
    }

    // 전체화면 뷰어
    if (showViewer && detail.photoUrls.isNotEmpty()) {
        PhotoViewerDialog(
            photoUrls = detail.photoUrls,
            initialPage = initialPage,
            isUploading = state.isPhotoUploading,
            onDismiss = { showViewer = false },
            onDelete = { url -> viewModel.deletePhoto(url) },
            onAddPhotos = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        )
    }

    // 사진 없을 때 갤러리 런처 직접 실행 후 뷰어 닫기
    if (showViewer && detail.photoUrls.isEmpty()) {
        LaunchedEffect(Unit) {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            showViewer = false
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 사진 캐러셀
        PhotoCarousel(
            photoUrls = detail.photoUrls,
            startTime = detail.startTime,
            photoEmotions = detail.diary?.photoEmotions,
            onPhotoClick = { page ->
                initialPage = page
                showViewer = true
            },
            onAddPhotos = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
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
    startTime: String,
    photoEmotions: Map<String, String>? = null,
    onPhotoClick: (Int) -> Unit = {},
    onAddPhotos: () -> Unit = {}
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
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onPhotoClick(page) }
                )
            } else {
                // 사진 없을 때 플레이스홀더 (클릭하면 사진 추가)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF0EEE8))
                        .clickable { onAddPhotos() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.photo),
                            contentDescription = "기본 사진",
                            modifier = Modifier.size(72.dp),
                            contentScale = ContentScale.Fit
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "사진 추가",
                                fontSize = 14.sp,
                                color = TextGray
                            )
                        }
                    }
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDate(startTime),
                        fontSize = 11.sp,
                        color = White.copy(alpha = 0.85f)
                    )
                }
                Text(
                    text = formatTime(startTime),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White
                )
            }
        }

        // 감정 태그 (좌측 상단) - 현재 사진의 감정 표시
        if (photoEmotions != null && photoUrls.isNotEmpty()) {
            val currentUrl = photoUrls.getOrNull(pagerState.currentPage)
            val emotionTag = currentUrl?.let { photoEmotions[it] }
            if (emotionTag != null) {
                val tagColor = when (emotionTag) {
                    "행복" -> Color(0xFFFFA726)
                    "편안" -> Color(0xFF66BB6A)
                    "슬픔" -> Color(0xFF42A5F5)
                    "화남" -> Color(0xFFEF5350)
                    else -> Color.White
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tagColor.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = emotionTag,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
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
                value = if (detail.durationMinutes > 0) "${detail.durationMinutes}분"
                       else "${detail.durationSeconds}초"
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

@Composable
private fun PhotoViewerDialog(
    photoUrls: List<String>,
    initialPage: Int,
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onAddPhotos: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { photoUrls.size }
    )
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("사진 삭제") },
            text = { Text("이 사진을 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(photoUrls[pagerState.currentPage])
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 사진 풀스크린 페이저
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = photoUrls[page],
                    contentDescription = "사진 ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 상단 바 (X | 페이지 수 | 🗑️) 한 줄로
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    "${pagerState.currentPage + 1} / ${photoUrls.size}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = White
                )
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // 하단 추가 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = White)
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(White.copy(alpha = 0.15f))
                            .clickable { onAddPhotos() }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = White, modifier = Modifier.size(20.dp))
                        Text("사진 추가", color = White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

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
            // 헤더 + 감정 태그
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = diaryTitle(dogName),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PointGreen,
                    fontFamily = mansehFont
                )

                // 감정 태그 표시
                if (diary?.emotionTag != null) {
                    val tagColor = when (diary.emotionTag) {
                        "행복" -> Color(0xFFFFA726)
                        "편안" -> Color(0xFF66BB6A)
                        "슬픔" -> Color(0xFF42A5F5)
                        "화남" -> Color(0xFFEF5350)
                        else -> PointGreen
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = tagColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = diary.emotionTag,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tagColor,
                            fontFamily = mansehFont
                        )
                    }
                }
            }

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

/** 사진을 최대 1920px, JPEG 80% 품질로 압축 (413 방지) */
private fun compressImage(context: android.content.Context, uri: Uri, maxDimension: Int = 1920, quality: Int = 80): ByteArray? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (original == null) return null

        val ratio = minOf(maxDimension.toFloat() / original.width, maxDimension.toFloat() / original.height, 1f)
        val scaled = if (ratio < 1f) {
            android.graphics.Bitmap.createScaledBitmap(original, (original.width * ratio).toInt(), (original.height * ratio).toInt(), true)
        } else original

        val output = java.io.ByteArrayOutputStream()
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, output)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        output.toByteArray()
    } catch (e: Exception) {
        null
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
