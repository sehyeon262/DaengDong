package com.frontend.ui.screen.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.frontend.R
import com.frontend.ui.screen.record.components.CalendarSection
import com.frontend.ui.screen.record.components.MonthlySummarySection
import com.frontend.ui.screen.record.components.WalkRecordCard
import com.frontend.ui.theme.Background
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextBrown
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import java.time.LocalDate

@Composable
fun RecordScreen(
    onWalkClick: (Long) -> Unit = {},
    viewModel: RecordViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error ?: "오류가 발생했습니다",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadCalendar() }) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            state.calendar != null -> {
                val calendar = state.calendar!!
                val yearMonth = viewModel.getCurrentYearMonth()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 헤더 (강아지 프로필 + 제목)
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        RecordHeader(
                            dogName = state.dogName,
                            dogProfileImageUrl = state.dogProfileImageUrl
                        )
                    }

                // 월 통계
                item {
                    MonthlySummarySection(summary = calendar.summary)
                }

                // 캘린더
                item {
                    CalendarSection(
                        yearMonth = yearMonth,
                        walkDays = calendar.days,
                        selectedDate = state.selectedDate,
                        onDateSelected = { date -> viewModel.loadDailyRecords(date) },
                        onPreviousMonth = { viewModel.goToPreviousMonth() },
                        onNextMonth = { viewModel.goToNextMonth() }
                    )
                }

                // 선택 날짜 산책 기록 헤더
                state.selectedDate?.let { date ->
                    item {
                        val parts = date.split("-")
                        Text(
                            text = "${parts[1].toInt()}월 ${parts[2].toInt()}일의 산책",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMain,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // 로딩 중
                if (state.isDailyLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // 산책 카드 리스트
                val walks = state.dailyRecords?.walks.orEmpty()
                if (!state.isDailyLoading && walks.isEmpty() && state.selectedDate != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "이 날은 산책 기록이 없어요",
                                fontSize = 14.sp,
                                color = TextGray
                            )
                        }
                    }
                } else {
                    items(walks, key = { it.recordId }) { walk ->
                        WalkRecordCard(
                            walk = walk,
                            onClick = { onWalkClick(walk.recordId) }
                        )
                    }
                }

                // 하단 여백
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } // LazyColumn
        } // state.calendar != null
    } // when
    } // Box
} // RecordScreen

@Composable
private fun RecordHeader(
    dogName: String,
    dogProfileImageUrl: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = dogProfileImageUrl ?: R.drawable.default_profile,
            contentDescription = "강아지 프로필",
            placeholder = painterResource(R.drawable.default_profile),
            error = painterResource(R.drawable.default_profile),
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(1.dp, PointGreen.copy(alpha = 0.3f), CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            if (dogName.isNotEmpty()) {
                Text(
                    text = "${nameWithJosa(dogName)} 함께한",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBrown
                )
            }
            Text(
                text = "산책 내역",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
        }
    }
}

// 받침 여부에 따라 '와' / '이와' 선택
private fun nameWithJosa(name: String): String {
    if (name.isEmpty()) return name
    val lastChar = name.last()
    val code = lastChar.code
    // 한글 완성형 범위: AC00 ~ D7A3, 종성 인덱스 = (code - 0xAC00) % 28
    return if (code in 0xAC00..0xD7A3 && (code - 0xAC00) % 28 != 0) {
        "${name}이와"
    } else {
        "${name}와"
    }
}
