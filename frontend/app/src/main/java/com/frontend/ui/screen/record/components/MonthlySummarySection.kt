package com.frontend.ui.screen.record.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frontend.domain.model.CalendarSummary
import com.frontend.ui.theme.Dimens
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import com.frontend.ui.theme.White

@Composable
fun MonthlySummarySection(summary: CalendarSummary) {
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
            .padding(vertical = 20.dp, horizontal = 16.dp)
    ) {
        Text(
            text = "이번 달 통계",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMain
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(
                label = "산책률",
                value = "${summary.walkRate}%",
                valueColor = PointGreen
            )
            SummaryItem(
                label = "총 시간",
                value = formatDuration(summary.totalDurationMinutes)
            )
            SummaryItem(
                label = "총 거리",
                value = "${"%.1f".format(summary.totalDistanceKm)}km"
            )
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextMain
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusSmall))
            .background(Color(0xFFFFFEF8))
            .border(
                width = 1.dp,
                color = Color(0xFFCCA040).copy(alpha = 0.15f),
                shape = RoundedCornerShape(Dimens.RadiusSmall)
            )
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextGray
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
