package com.frontend.ui.screen.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frontend.domain.model.HomeWeeklySummary
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import kotlin.math.abs

@Composable
fun WeeklyReportSection(weeklySummary: HomeWeeklySummary?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, PointGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 타이틀 ────────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "주간 리포트",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextGray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (weeklySummary == null) {
            Text(
                text = "아직 산책 기록이 없어요!",
                fontSize = 13.sp,
                color = TextGray
            )
        } else {
            val stats = weeklySummary.weeklyStats ?: emptyList()
            val maxDistance = stats.maxOfOrNull { it.distance }?.coerceAtLeast(1) ?: 1
            val maxBarHeight = 90.dp

            // ── 막대 차트 ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.forEach { stat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        val barHeight = (stat.distance.toFloat() / maxDistance * maxBarHeight.value).dp
                            .coerceAtLeast(4.dp)

                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            PointGreen.copy(alpha = if (stat.distance == maxDistance) 0.85f else 0.5f),
                                            PointGreen.copy(alpha = if (stat.distance == maxDistance) 0.3f else 0.15f)
                                        )
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stat.day,
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 지난주 대비 메시지 (중앙, 포맷 적용) ────────────────────────
            val diffText = buildDiffMessage(weeklySummary.diffDistance)
            Text(
                text = "🐾  $diffText",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray
            )
        }
    }
}

// ── m → km 포맷 변환 ──────────────────────────────────────────────────────────
// 1000m 미만: "800m" / 이상: "2.6km", "3km"
private fun formatDistance(meters: Int): String {
    if (meters < 1000) return "${meters}m"
    val km = meters / 1000.0
    val formatted = "%.1f".format(km).trimEnd('0').trimEnd('.')
    return "${formatted}km"
}

// ── 지난주 대비 메시지 생성 ────────────────────────────────────────────────────
private fun buildDiffMessage(diffDistance: Int): String {
    return when {
        diffDistance > 0  -> "지난주보다 ${formatDistance(diffDistance)} 더 걸었어요"
        diffDistance < 0  -> "지난주보다 ${formatDistance(abs(diffDistance))} 덜 걸었어요"
        else              -> "지난주와 비슷하게 걸었어요"
    }
}
