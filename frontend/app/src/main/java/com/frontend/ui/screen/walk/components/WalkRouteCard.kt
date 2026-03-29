package com.frontend.ui.screen.walk.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Room
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frontend.domain.model.WalkRoute
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import java.util.Locale

@Composable
fun WalkRouteCard(
    route: WalkRoute,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardAlpha = if (isSelected) 1f else 0.5f

    Box(
        modifier = modifier
            .alpha(cardAlpha)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(
                border = if (isSelected) {
                    BorderStroke(2.dp, PointGreen)
                } else {
                    BorderStroke(0.dp, Color.Transparent)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                var subtitleFontSize by remember(route.subtitle) { mutableStateOf(12.sp) }
                Text(
                    text = route.subtitle,
                    fontSize = subtitleFontSize,
                    color = if (isSelected) PointGreen else TextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    onTextLayout = { result ->
                        if (result.hasVisualOverflow && subtitleFontSize > 9.sp) {
                            subtitleFontSize = (subtitleFontSize.value - 0.5f).sp
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                var titleFontSize by remember(route.title) { mutableStateOf(18.sp) }
                Text(
                    text = route.title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    onTextLayout = { result ->
                        if (result.hasVisualOverflow && titleFontSize > 10.sp) {
                            titleFontSize = (titleFontSize.value - 1).sp
                        }
                    }
                )

                // 항상 공간을 예약해서 모든 카드의 높이를 동일하게 유지한다.
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 16.dp)
                ) {
                    route.distanceKm?.let { distance ->
                        Icon(
                            imageVector = Icons.Filled.Room,
                            contentDescription = "거리",
                            tint = TextGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Text(
                            text = String.format(Locale.US, "%.2fkm", distance),
                            fontSize = 12.sp,
                            color = TextGray,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    route.durationMin?.let { duration ->
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "소요시간",
                            tint = TextGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${duration}분",
                            fontSize = 12.sp,
                            color = TextGray,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PointGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = "산책",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
