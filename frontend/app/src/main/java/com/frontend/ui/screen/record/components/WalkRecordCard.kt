package com.frontend.ui.screen.record.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.frontend.domain.model.WalkItem
import com.frontend.ui.theme.CardBeige
import com.frontend.ui.theme.Dimens
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import com.frontend.ui.theme.White

@Composable
fun WalkRecordCard(
    walk: WalkItem,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMedium))
            .background(White)
            .border(
                width = 1.dp,
                color = PointGreen.copy(alpha = 0.3f),
                shape = RoundedCornerShape(Dimens.RadiusMedium)
            )
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 썸네일
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(Dimens.RadiusSmall))
                .background(CardBeige)
        ) {
            if (walk.thumbnailUrl != null) {
                AsyncImage(
                    model = walk.thumbnailUrl,
                    contentDescription = "산책 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp)
                )
            } else {
                // 기본 아이콘
                Text(
                    text = "\uD83D\uDC3E",
                    fontSize = 28.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // 산책 정보
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 시간
            Text(
                text = formatTime(walk.startedAt),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain
            )

            // 시간 + 거리
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TextGray
                    )
                    Text(
                        text = "${walk.durationMinutes}분",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TextGray
                    )
                    Text(
                        text = "${walk.distanceKm} km",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
            }
        }
    }
}

private fun formatTime(dateTimeStr: String): String {
    return try {
        // "2026-03-12T16:30:00" -> "오후 4:30"
        val time = dateTimeStr.substringAfter("T")
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1]
        val amPm = if (hour < 12) "오전" else "오후"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        "$amPm $displayHour:$minute"
    } catch (e: Exception) {
        dateTimeStr
    }
}
