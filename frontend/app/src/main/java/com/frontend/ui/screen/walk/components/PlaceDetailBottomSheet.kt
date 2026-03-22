package com.frontend.ui.screen.walk.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.frontend.R
import com.frontend.domain.model.Place
import com.frontend.ui.theme.Dimens
import com.frontend.ui.theme.IconGray
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import com.frontend.ui.theme.White

private val ScrimColor = Color.Black.copy(alpha = 0.4f)

@Composable
fun PlaceDetailBottomSheet(
    place: Place,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // 스크림 — 탭하면 닫힘
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // 바텀시트 패널
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = Dimens.RadiusLarge, topEnd = Dimens.RadiusLarge),
            color = White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {

                // ── 이미지 영역 ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = place.imageUrl,
                        contentDescription = place.name,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.place_mark),
                        error = painterResource(R.drawable.place_mark),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    topStart = Dimens.RadiusLarge,
                                    topEnd = Dimens.RadiusLarge
                                )
                            )
                    )
                    // 이미지 위 그라디언트 (가독성을 위해 하단을 살짝 어둡게)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    topStart = Dimens.RadiusLarge,
                                    topEnd = Dimens.RadiusLarge
                                )
                            )
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                    // 닫기 버튼 (이미지 우측 상단)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    // 거리 배지 (이미지 좌측 하단)
                    place.distanceMeters?.let { distance ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📍 ${formatDistance(distance)}",
                                fontSize = 12.sp,
                                color = White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ── 본문 콘텐츠 ──────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLarge)
                ) {
                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                    // 드래그 핸들
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                    // 카테고리 배지 + 장소명
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = PointGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = place.categoryName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PointGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = place.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )

                    Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                    // ── 구분선 ───────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFF0F0F0))
                    )

                    Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                    // ── 주소 ─────────────────────────────────────────────────
                    PlaceInfoRow(
                        icon = Icons.Filled.LocationOn,
                        text = place.address.ifBlank { "주소 정보 없음" }
                    )

                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                    // ── 연락처 ───────────────────────────────────────────────
                    PlaceInfoRow(
                        icon = Icons.Filled.Phone,
                        text = place.contact.ifBlank { "연락처 정보 없음" }
                    )

                    // ── 설명 (있을 경우만 표시) ──────────────────────────────
                    place.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF0F0F0))
                        )

                        Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = PointGreen.copy(alpha = 0.07f),
                                    shape = RoundedCornerShape(Dimens.RadiusMedium)
                                )
                                .padding(Dimens.SpacingMedium),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 소개 헤더
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = PointGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "소개",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PointGreen
                                )
                            }
                            // 설명 본문
                            Text(
                                text = desc,
                                fontSize = Dimens.TextSizeSmall,
                                color = TextMain,
                                lineHeight = 22.sp,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.SpacingXLarge))
                }
            }
        }
    }
}

// ── 정보 행 컴포넌트 (아이콘 + 텍스트) ────────────────────────────────────────
@Composable
private fun PlaceInfoRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = PointGreen.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PointGreen,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = Dimens.TextSizeSmall,
            color = TextMain,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters < 1000) {
        "${meters.toInt()}m"
    } else {
        String.format("%.1fkm", meters / 1000)
    }
}
