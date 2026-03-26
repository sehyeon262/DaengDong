package com.frontend.ui.screen.dog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.frontend.R
import com.frontend.domain.model.MetDogResponse
import com.frontend.ui.theme.Background
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── 피드백 색상 (부드럽게) ─────────────────────────────────────────────────────
private val GoodColor = Color(0xFF66BB6A)
private val BadColor  = Color(0xFFEF5350)
private val GoodBg    = Color(0xFFF2FBF2)
private val BadBg     = Color(0xFFFFF2F2)

// ── 날짜 필터 옵션 ─────────────────────────────────────────────────────────────
enum class DateFilter(val label: String) {
    ALL("전체"),
    WEEK("7일"),
    MONTH("30일"),
    THREE_MONTHS("3개월")
}

// ─────────────────────────────────────────────────────────────────────────────
// MetDogsScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetDogsScreen(
    navController: NavController,
    viewModel: MetDogsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab        by remember { mutableIntStateOf(0) }
    var selectedDateFilter by remember { mutableStateOf(DateFilter.ALL) }
    var feedbackTarget     by remember { mutableStateOf<MetDogResponse?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val tabs = listOf("최근 만남", "역대 목록")

    // ── 피드백 바텀시트 ────────────────────────────────────────────────────────
    if (feedbackTarget != null) {
        ModalBottomSheet(
            onDismissRequest = { feedbackTarget = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            FeedbackBottomSheet(
                dogName = feedbackTarget!!.targetDogName,
                currentFeedback = feedbackTarget!!.feedback,
                onSelect = { feedback ->
                    viewModel.updateFeedback(
                        targetDogId  = feedbackTarget!!.targetDogId,
                        walkRecordId = feedbackTarget!!.lastWalkRecordId,
                        feedback     = feedback
                    )
                    feedbackTarget = null
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── 상단 바 ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextMain
                )
            }
            Spacer(Modifier.weight(1f))
            Text("만난 친구들", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        // ── 탭 (최근 만남 / 역대 목록) ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(25.dp))
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(25.dp))
                            .background(
                                if (selectedTab == index) PointGreen else Color.Transparent
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTab == index) Color.White else TextGray
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 본문 ───────────────────────────────────────────────────────────────
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PointGreen)
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error!!, color = TextGray, fontSize = 14.sp)
                }
            }
            else -> {
                val isRecentTab = selectedTab == 0
                val dogs = if (isRecentTab) {
                    state.recentMetDogs
                } else {
                    filterByDate(state.allMetDogs, selectedDateFilter)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ① 안내 배너 — 항상 최상단 (탭 공통)
                    item { InfoBanner() }

                    // ② 날짜 필터 칩 — 역대 목록 전용
                    if (!isRecentTab) {
                        item {
                            DateFilterRow(
                                selected = selectedDateFilter,
                                onSelect = { selectedDateFilter = it }
                            )
                        }
                    }

                    if (dogs.isEmpty()) {
                        // ③-A 빈 상태
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Pets,
                                        contentDescription = null,
                                        tint = TextGray.copy(alpha = 0.35f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = if (isRecentTab) "최근 산책에서 만난 친구가 없어요"
                                               else "조건에 해당하는 친구가 없어요",
                                        color = TextGray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // ③-B 섹션 헤더
                        item {
                            Text(
                                text = if (isRecentTab) "최근 산책에서 만났어요"
                                       else "지금까지 만난 친구들",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMain
                            )
                        }

                        if (isRecentTab) {
                            // ④ 최근 만남 — 단순 목록
                            items(dogs, key = { it.metDogId }) { dog ->
                                MetDogCard(
                                    dog = dog,
                                    showTime = true,
                                    onFeedbackClick = { feedbackTarget = dog }
                                )
                            }
                        } else {
                            // ④ 역대 목록 — 날짜별 그룹
                            val grouped = dogs
                                .groupBy { getDateStr(it.lastMetAt) }
                                .entries
                                .sortedByDescending { it.key }

                            grouped.forEach { (dateStr, dogsOnDate) ->
                                item(key = "header_$dateStr") {
                                    DateGroupHeader(dateStr)
                                }
                                items(dogsOnDate, key = { "dog_${it.metDogId}" }) { dog ->
                                    MetDogCard(
                                        dog = dog,
                                        showTime = false,
                                        onFeedbackClick = { feedbackTarget = dog }
                                    )
                                }
                            }
                        }

                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 날짜 필터 칩 행
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DateFilterRow(
    selected: DateFilter,
    onSelect: (DateFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateFilter.values().forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) PointGreen else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) PointGreen else Color(0xFFDDDDDD),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextGray
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 날짜 그룹 헤더 (역대 목록)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DateGroupHeader(dateStr: String) {
    Text(
        text = dateStr,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = TextGray,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 강아지 카드
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MetDogCard(
    dog: MetDogResponse,
    showTime: Boolean,
    onFeedbackClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 프로필 이미지 — null이면 default_profile 사용 (발바닥 X)
                AsyncImage(
                    model = dog.targetDogProfileImageUrl,
                    contentDescription = "${dog.targetDogName} 프로필",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.default_profile),
                    error = painterResource(R.drawable.default_profile)
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = dog.targetDogName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMain
                        )
                        if (dog.targetDogBreed != null) {
                            BreedChip(dog.targetDogBreed)
                        }
                    }
                    Text(
                        text = formatMetTime(dog.lastMetAt, showTime),
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // 궁합 평가 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFeedbackClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (!dog.feedbackDone) {
                    Text("궁합 평가하기", fontSize = 14.sp, color = TextGray)
                } else {
                    when (dog.feedback) {
                        "좋아요" -> FeedbackTag("잘 맞아요", GoodBg, GoodColor)
                        "싫어요" -> FeedbackTag("안 맞아요", BadBg, BadColor)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 종(breed) 칩 — 연한 베이지 톤 (기존 형광 노란색 제거)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BreedChip(breed: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFF2EFE8), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = breed,
            fontSize = 11.sp,
            color = Color(0xFF7A6A50),
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 피드백 완료 태그
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FeedbackTag(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 13.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 피드백 바텀시트 — OutlinedButton으로 부드럽게
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FeedbackBottomSheet(
    dogName: String,
    currentFeedback: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${dogName}와의 궁합은?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 잘 맞아요 버튼
            val goodSelected = currentFeedback == "좋아요"
            OutlinedButton(
                onClick = { onSelect("좋아요") },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (goodSelected) GoodBg else Color.Transparent,
                    contentColor = GoodColor
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = GoodColor.copy(alpha = if (goodSelected) 1f else 0.4f)
                )
            ) {
                Text(
                    text = "잘 맞아요",
                    fontSize = 15.sp,
                    fontWeight = if (goodSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = GoodColor
                )
            }

            // 안 맞아요 버튼
            val badSelected = currentFeedback == "싫어요"
            OutlinedButton(
                onClick = { onSelect("싫어요") },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (badSelected) BadBg else Color.Transparent,
                    contentColor = BadColor
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = BadColor.copy(alpha = if (badSelected) 1f else 0.4f)
                )
            ) {
                Text(
                    text = "안 맞아요",
                    fontSize = 15.sp,
                    fontWeight = if (badSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = BadColor
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 안내 배너 — 그린 톤으로 앱 색상 통일
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEDF7ED), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = PointGreen,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "궁합을 평가해주시면, 다음 산책 시 잘 맞는 친구를 만날 확률이 높아지고 안 맞는 친구는 경고 알림을 드려요!",
            fontSize = 13.sp,
            color = Color(0xFF2E7D32),
            lineHeight = 18.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 유틸 함수
// ─────────────────────────────────────────────────────────────────────────────

/** 날짜 필터 적용 */
private fun filterByDate(
    dogs: List<MetDogResponse>,
    filter: DateFilter
): List<MetDogResponse> {
    if (filter == DateFilter.ALL) return dogs
    val cutoff = when (filter) {
        DateFilter.WEEK         -> LocalDateTime.now().minusDays(7)
        DateFilter.MONTH        -> LocalDateTime.now().minusDays(30)
        DateFilter.THREE_MONTHS -> LocalDateTime.now().minusMonths(3)
        DateFilter.ALL          -> return dogs
    }
    return dogs.filter {
        try {
            LocalDateTime.parse(it.lastMetAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME) >= cutoff
        } catch (e: Exception) {
            true
        }
    }
}

/** lastMetAt → "yyyy.MM.dd" */
private fun getDateStr(dateTimeStr: String): String {
    return try {
        LocalDateTime
            .parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    } catch (e: Exception) {
        dateTimeStr
    }
}

/** 표시용 시간 포맷 */
private fun formatMetTime(dateTimeStr: String, showTime: Boolean): String {
    return try {
        val dt = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        if (showTime) {
            dt.format(DateTimeFormatter.ofPattern("a h:mm에 만남", Locale.KOREAN))
        } else {
            dt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd에 만남"))
        }
    } catch (e: Exception) {
        dateTimeStr
    }
}
