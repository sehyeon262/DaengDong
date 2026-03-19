package com.frontend.ui.screen.record.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.drawBehind
import com.frontend.R
import com.frontend.domain.model.DayWalkCount
import com.frontend.ui.theme.Dimens
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import com.frontend.ui.theme.White
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarSection(
    yearMonth: YearMonth,
    walkDays: List<DayWalkCount>,
    selectedDate: String?,
    onDateSelected: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val walkDaySet = walkDays.associate { it.date to it.walkCount }
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    // 일요일 = 0, 월요일 = 1, ..., 토요일 = 6
    val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value % 7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMedium))
            .background(White)
            .border(
                width = 1.dp,
                color = PointGreen.copy(alpha = 0.4f),
                shape = RoundedCornerShape(Dimens.RadiusMedium)
            )
            .padding(16.dp)
    ) {
        // 월 네비게이션 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전 달",
                    tint = TextGray
                )
            }
            Text(
                text = "${yearMonth.year}년 ${yearMonth.monthValue}월",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음 달",
                    tint = TextGray
                )
            }
        }

        // 요일 헤더
        val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 날짜 그리드
        val totalCells = startDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - startDayOfWeek + 1

                    if (day in 1..daysInMonth) {
                        val dateStr = LocalDate.of(yearMonth.year, yearMonth.monthValue, day)
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val hasWalk = walkDaySet.containsKey(dateStr)
                        val isSelected = dateStr == selectedDate
                        val isToday = dateStr == LocalDate.now().toString()

                        CalendarDay(
                            day = day,
                            hasWalk = hasWalk,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onDateSelected(dateStr) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // 빈 셀
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    hasWalk: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hasWalk) {
            // 산책한 날 → face.png 크게, 선택 시 아주 연한 빛만
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (isSelected)
                            Modifier.drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            PointGreen.copy(alpha = 0.6f),
                                            PointGreen.copy(alpha = 0.3f),
                                            PointGreen.copy(alpha = 0.0f)
                                        ),
                                        radius = size.minDimension / 2f * 1.4f
                                    ),
                                    radius = size.minDimension / 2f * 1.4f
                                )
                            }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.face),
                    contentDescription = "산책 완료",
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // 산책 없는 날 → 숫자만
            Text(
                text = "$day",
                fontSize = 13.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> PointGreen
                    else -> TextGray.copy(alpha = 0.55f)
                }
            )
        }
    }
}
