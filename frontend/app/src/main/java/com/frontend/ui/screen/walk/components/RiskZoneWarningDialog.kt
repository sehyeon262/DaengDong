package com.frontend.ui.screen.walk.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frontend.domain.model.NearbyDangerZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val WarningOrange = Color(0xFFFF9500)
private val PulseOrange = Color(0xFFFFB347)

/**
 * 현재 위치에서 목표 지점까지의 방향을 8방향으로 반환
 */
private fun calculateDirection(
    currentLat: Double,
    currentLon: Double,
    targetLat: Double,
    targetLon: Double
): String {
    val dLon = Math.toRadians(targetLon - currentLon)
    val lat1 = Math.toRadians(currentLat)
    val lat2 = Math.toRadians(targetLat)

    val x = sin(dLon) * cos(lat2)
    val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

    var bearing = Math.toDegrees(atan2(x, y))
    bearing = (bearing + 360) % 360  // 0 ~ 360 범위로 정규화

    return when {
        bearing >= 337.5 || bearing < 22.5 -> "북쪽"
        bearing >= 22.5 && bearing < 67.5 -> "북동쪽"
        bearing >= 67.5 && bearing < 112.5 -> "동쪽"
        bearing >= 112.5 && bearing < 157.5 -> "남동쪽"
        bearing >= 157.5 && bearing < 202.5 -> "남쪽"
        bearing >= 202.5 && bearing < 247.5 -> "남서쪽"
        bearing >= 247.5 && bearing < 292.5 -> "서쪽"
        else -> "북서쪽"
    }
}

@Composable
fun RiskZoneWarningDialog(
    zone: NearbyDangerZone,
    currentLatitude: Double?,
    currentLongitude: Double?,
    onDismiss: () -> Unit,
) {
    // 펄스 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // 화면 전체를 반투명 배경으로 덮고, 아무 곳이나 터치하면 닫힘
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // 다이얼로그 카드
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* 카드 터치는 배경 클릭 방지용 소비 */ },
                )
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 펄스 애니메이션이 있는 경고 아이콘
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 펄스 링 (바깥쪽)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .graphicsLayer { alpha = pulseAlpha }
                        .background(PulseOrange, CircleShape)
                )
                // 메인 원
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(WarningOrange, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "위험",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "위험 장소 접근",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "내가 등록한 위험 장소에\n접근 중이에요",
                fontSize = 15.sp,
                color = Color(0xFF555577),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(20.dp))

            // 위험 사유 표시
            val reasonText = zone.customReason?.let { "${zone.reason.label}: $it" } ?: zone.reason.label
            Text(
                text = "사유: $reasonText",
                fontSize = 15.sp,
                color = Color(0xFF1A1A2E),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            // 방향 + 거리 표시
            val directionText = if (currentLatitude != null && currentLongitude != null) {
                val direction = calculateDirection(
                    currentLatitude, currentLongitude,
                    zone.location.latitude, zone.location.longitude
                )
                "$direction ${zone.distanceM.toInt()}m"
            } else {
                "${zone.distanceM.toInt()}m"
            }

            Text(
                text = directionText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = WarningOrange,
            )
        }
    }
}
