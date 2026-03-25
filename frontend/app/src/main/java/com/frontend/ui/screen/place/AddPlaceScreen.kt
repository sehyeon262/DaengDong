package com.frontend.ui.screen.place

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.frontend.domain.model.KakaoPlace
import com.frontend.ui.theme.Background
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kotlinx.coroutines.launch

private val CATEGORIES = listOf("식당", "카페", "위탁관리", "여행지", "동물병원", "동물약국", "펜션", "호텔")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPlaceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddPlaceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 갤러리에서 이미지 선택
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.onImageSelected(it) } }

    // 갤러리 권한 요청
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) imageLauncher.launch("image/*")
        else scope.launch { snackbarHostState.showSnackbar("사진 접근 권한이 필요합니다") }
    }

    // 위치 권한 요청 — 허용 시 현재 위치로 지도 초기화 + 역지오코딩으로 주소 자동 입력
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) getLastKnownLocation(context) { lat, lon ->
            viewModel.onCurrentLocationObtained(lat, lon)
        }
    }

    // 화면 진입 시 위치 권한 확인 → 있으면 바로 현재 위치 로드, 없으면 요청
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            getLastKnownLocation(context) { lat, lon ->
                viewModel.onCurrentLocationObtained(lat, lon)
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 등록 성공 시 뒤로 이동
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            snackbarHostState.showSnackbar("장소가 등록되었습니다!")
            onNavigateBack()
        }
    }

    // 에러 메시지 스낵바
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 주소 검색 다이얼로그
    if (state.isAddressSearchOpen) {
        AddressSearchDialog(
            query = state.searchQuery,
            results = state.searchResults,
            isSearching = state.isSearching,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onSelect = { viewModel.onPlaceSelected(it) },
            onDismiss = { viewModel.closeAddressSearch() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "신규장소 등록하기",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextMain
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. 이미지 업로드 영역 ──────────────────────────────────────
            ImageUploadSection(
                imageUri = state.imageUri,
                onPickImage = {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.READ_MEDIA_IMAGES
                    else
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    permissionLauncher.launch(permission)
                },
                onRemoveImage = { viewModel.onImageRemoved() }
            )

            // ── 2. 장소명 입력 ──────────────────────────────────────────────
            SectionLabel(text = "장소명")
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onNameChange(it) },
                placeholder = { Text("장소명을 입력해주세요", color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            // ── 3. 위치 ──────────────────────────────────────────────────
            SectionLabel(text = "위치")

            // 주소 찾기 — 읽기 전용, 클릭 시 검색 다이얼로그 오픈
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { viewModel.openAddressSearch() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "주소 검색",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.selectedAddress.ifEmpty { "주소 찾기" },
                        fontSize = 14.sp,
                        color = if (state.selectedAddress.isEmpty()) TextGray else TextMain
                    )
                }
            }

            // 상세주소 입력
            OutlinedTextField(
                value = state.detailAddress,
                onValueChange = { viewModel.onDetailAddressChange(it) },
                placeholder = { Text("상세주소를 입력해주세요.", color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            // 카카오 미니 지도 — 탭하면 역지오코딩으로 주소 자동 갱신
            MiniKakaoMap(
                latitude = state.latitude,
                longitude = state.longitude,
                onMapTapped = { lat, lon -> viewModel.onMapTapped(lat, lon) }
            )
            Text(
                text = "지도를 탭하면 위치를 변경할 수 있어요",
                fontSize = 12.sp,
                color = TextGray,
                modifier = Modifier.padding(start = 4.dp)
            )

            // ── 4. 업종 선택 ────────────────────────────────────────────────
            SectionLabel(text = "업종")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.forEach { category ->
                    val selected = state.selectedCategory == category
                    SuggestionChip(
                        onClick = { viewModel.onCategorySelect(category) },
                        label = { Text(category, fontSize = 14.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (selected) PointGreen else Color(0xFFF5F5DC),
                            labelColor = if (selected) Color.White else TextMain
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (selected) PointGreen else Color(0xFFD4C9A8)
                        )
                    )
                }
            }

            // ── 5. 장소 설명 ────────────────────────────────────────────────
            SectionLabel(text = "장소 설명")
            OutlinedTextField(
                value = state.memo,
                onValueChange = { viewModel.onMemoChange(it) },
                placeholder = { Text("장소를 설명해주세요.", color = TextGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── 6. 저장 버튼 ────────────────────────────────────────────────
            Button(
                onClick = { viewModel.registerPlace(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PointGreen,
                    disabledContainerColor = PointGreen.copy(alpha = 0.6f)
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── 주소 검색 다이얼로그 ──────────────────────────────────────────────────────
@Composable
private fun AddressSearchDialog(
    query: String,
    results: List<KakaoPlace>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (KakaoPlace) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 다이얼로그 상단 바
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "닫기", tint = TextMain)
                    }
                    Text(
                        text = "주소 검색",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                }

                // 검색 입력 필드
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("건물명, 도로명, 지번 검색", color = TextGray) },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PointGreen
                            )
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = "검색", tint = TextGray)
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "지우기", tint = TextGray)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors()
                )

                // 검색 결과 목록
                if (results.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                results.take(8).forEachIndexed { index, place ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelect(place) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = place.placeName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextMain
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = place.roadAddressName.ifEmpty { place.addressName },
                                            fontSize = 12.sp,
                                            color = TextGray
                                        )
                                    }
                                    if (index < results.take(8).lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.5.dp)
                                                .background(Color(0xFFEEEEEE))
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (query.length >= 2 && !isSearching) {
                    // 검색어 있는데 결과 없는 경우
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("검색 결과가 없습니다", fontSize = 14.sp, color = TextGray)
                    }
                }
            }
        }
    }

    // 다이얼로그 열리면 키보드 자동 포커스
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

// ── 이미지 업로드 영역 ────────────────────────────────────────────────────────
@Composable
private fun ImageUploadSection(
    imageUri: android.net.Uri?,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("장소 사진 업로드", fontSize = 13.sp, color = TextGray)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF0EDE6))
                .border(1.dp, Color(0xFFE0D9CC), RoundedCornerShape(12.dp))
                .clickable { onPickImage() },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "선택된 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 삭제 버튼
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onRemoveImage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "이미지 삭제",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "사진 추가",
                        modifier = Modifier.size(48.dp),
                        tint = PointGreen
                    )
                }
                // 카메라 아이콘 (우하단)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PointGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── 섹션 레이블 (체크박스 스타일) ──────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PointGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain)
    }
}

// ── 카카오 미니 지도 ──────────────────────────────────────────────────────────
@Composable
private fun MiniKakaoMap(
    latitude: Double,
    longitude: Double,
    onMapTapped: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapStarted by remember { mutableStateOf(false) }
    // 탭으로 찍은 좌표는 카메라 이동 없이 마커만 업데이트
    var lastTapCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // 라이프사이클 연동 — resume/pause/finish 를 WalkScreen 과 동일하게 처리
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

    // 탭 리스너 등록
    LaunchedEffect(kakaoMap) {
        kakaoMap?.setOnMapClickListener { _, latLng, _, _ ->
            latLng?.let { onMapTapped(it.latitude, it.longitude) }
        }
    }

    // 80px 높이로 스케일된 마커 비트맵 (WalkScreen 과 동일한 방식)
    val markerBitmap = remember {
        val src = android.graphics.BitmapFactory.decodeResource(
            context.resources, com.frontend.R.drawable.place_marker
        )
        val targetH = 80
        val targetW = (targetH * src.width.toFloat() / src.height).toInt()
        android.graphics.Bitmap.createScaledBitmap(src, targetW, targetH, true)
    }

    // 좌표 변경 시 마커 업데이트 — 탭으로 찍은 경우 카메라 이동 없이 마커만 업데이트
    LaunchedEffect(latitude, longitude) {
        kakaoMap?.let { map ->
            val pos = LatLng.from(latitude, longitude)
            val isTap = lastTapCoords?.first == latitude && lastTapCoords?.second == longitude
            if (!isTap) {
                map.moveCamera(CameraUpdateFactory.newCenterPosition(pos, 15))
            }
            map.labelManager?.layer?.removeAll()
            map.labelManager?.layer?.addLabel(
                LabelOptions.from(pos)
                    .setStyles(LabelStyles.from(LabelStyle.from(markerBitmap).setAnchorPoint(0.5f, 1.0f)))
            )
        }
    }

    AndroidView(
        factory = { _ ->
            mapView.apply {
                // 지도 위 터치 시 부모 ScrollView 스크롤 차단
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false // 이벤트는 지도로 전달
                }
                runCatching {
                    start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {}
                            override fun onMapError(e: Exception) {
                                android.util.Log.e("MiniKakaoMap", "onMapError: ${e.message}", e)
                            }
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(map: KakaoMap) {
                                mapStarted = true
                                kakaoMap = map
                                val pos = LatLng.from(latitude, longitude)
                                map.moveCamera(CameraUpdateFactory.newCenterPosition(pos, 15))
                                map.labelManager?.layer?.addLabel(
                                    LabelOptions.from(pos)
                                        .setStyles(LabelStyles.from(LabelStyle.from(markerBitmap).setAnchorPoint(0.5f, 1.0f)))
                                )
                                map.setOnMapClickListener { _, latLng, _, _ ->
                                    latLng?.let {
                                        lastTapCoords = Pair(it.latitude, it.longitude)
                                        onMapTapped(it.latitude, it.longitude)
                                    }
                                }
                            }
                        }
                    )
                }.onFailure { e ->
                    android.util.Log.e("MiniKakaoMap", "MapView.start() 실패: ${e.message}", e)
                }
                if (mapStarted && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    runCatching { resume() }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

// ── 공통 TextField 색상 ────────────────────────────────────────────────────────
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = Color(0xFFDDDDDD),
    focusedBorderColor = PointGreen,
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White
)

// ── 현재 위치 조회 헬퍼 ─────────────────────────────────────────────────────────
@android.annotation.SuppressLint("MissingPermission")
private fun getLastKnownLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    try {
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                location?.let { onLocation(it.latitude, it.longitude) }
            }
    } catch (_: SecurityException) {
        // 권한이 없는 경우 무시 — 기본 좌표(서울 시청) 유지
    }
}
