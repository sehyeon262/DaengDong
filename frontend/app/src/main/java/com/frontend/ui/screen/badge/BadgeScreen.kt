package com.frontend.ui.screen.badge

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.frontend.R
import com.frontend.domain.model.BadgeProgressResponse
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain

import com.frontend.ui.theme.Background

private fun getBadgeDrawable(badgeId: Long): Int {
    return when (badgeId) {
        1L -> R.drawable.badge1
        2L -> R.drawable.badge2
        3L -> R.drawable.badge3
        4L -> R.drawable.badge4
        5L -> R.drawable.badge5
        6L -> R.drawable.badge6
        7L -> R.drawable.badge7
        8L -> R.drawable.badge8
        9L -> R.drawable.badge9
        else -> R.drawable.badge1
    }
}

// 흑백 필터 (미획득 배지용)
private val grayscaleMatrix = ColorFilter.colorMatrix(
    ColorMatrix().apply { setToSaturation(0f) }
)

@Composable
fun BadgeScreen(
    navController: NavController,
    viewModel: BadgeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedBadge by remember { mutableStateOf<BadgeProgressResponse?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextMain
                )
            }
            Text(
                "배지",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
        }

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PointGreen)
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("배지를 불러올 수 없습니다", color = TextGray, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(state.error!!, color = TextGray, fontSize = 12.sp)
                    }
                }
            }
            else -> {
                // 획득 개수 요약
                val earnedCount = state.badges.count { it.earned || it.currentValue >= it.targetValue }
                val totalCount = state.badges.size

                Text(
                    "$earnedCount / $totalCount 획득",
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                    fontSize = 14.sp,
                    color = TextGray
                )

                // 안내 배너
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp)
                        .background(
                            color = PointGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🐾", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (earnedCount == 0)
                                "산책하고, 기록하고, 친구 만들면 배지를 모을 수 있어요!"
                            else if (earnedCount < totalCount)
                                "잘하고 있어요! 조금만 더 하면 새로운 배지를 받을 수 있어요!"
                            else
                                "대단해요! 모든 배지를 모았어요! 🎉",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = PointGreen,
                            lineHeight = 18.sp
                        )
                    }
                }

                // 3열 그리드
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(state.badges) { badge ->
                        BadgeItem(
                            badge = badge,
                            onClick = { selectedBadge = badge }
                        )
                    }
                }
            }
        }
    }

    // 배지 상세 다이얼로그
    selectedBadge?.let { badge ->
        BadgeDetailDialog(
            badge = badge,
            onDismiss = { selectedBadge = null }
        )
    }
}

@Composable
private fun BadgeItem(
    badge: BadgeProgressResponse,
    onClick: () -> Unit
) {
    val isCompleted = badge.earned || badge.currentValue >= badge.targetValue

    Column(
        modifier = Modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 배지 이미지
        Box(
            modifier = Modifier.size(90.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(getBadgeDrawable(badge.badgeId)),
                contentDescription = badge.badgeName,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .then(if (!isCompleted) Modifier.alpha(0.4f) else Modifier),
                contentScale = ContentScale.Crop,
                colorFilter = if (!isCompleted) grayscaleMatrix else null
            )
        }

        Spacer(Modifier.height(6.dp))

        // 배지 이름
        Text(
            badge.badgeName,
            fontSize = 12.sp,
            fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCompleted) TextMain else TextGray,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 진행도 (미완료일 때만)
        if (!isCompleted) {
            Spacer(Modifier.height(4.dp))
            // 프로그레스 바
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                val progress = if (badge.targetValue > 0)
                    badge.currentValue.toFloat() / badge.targetValue else 0f
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(PointGreen, RoundedCornerShape(2.dp))
                )
            }
            Text(
                "${badge.currentValue} / ${badge.targetValue}",
                fontSize = 10.sp,
                color = TextGray
            )
        }
    }
}

@Composable
private fun BadgeDetailDialog(
    badge: BadgeProgressResponse,
    onDismiss: () -> Unit
) {
    val isCompleted = badge.earned || badge.currentValue >= badge.targetValue

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCompleted) {
                    Text(
                        "배지를 획득했어요!",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // 배지 이미지 (크게)
                Image(
                    painter = painterResource(getBadgeDrawable(badge.badgeId)),
                    contentDescription = badge.badgeName,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .then(if (!isCompleted) Modifier.alpha(0.4f) else Modifier),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (!isCompleted) grayscaleMatrix else null
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    badge.badgeName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    badge.description,
                    fontSize = 14.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                if (!isCompleted) {
                    Spacer(Modifier.height(16.dp))

                    // 진행도 바 (큰 버전)
                    val progress = if (badge.targetValue > 0)
                        badge.currentValue.toFloat() / badge.targetValue else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE0E0E0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .background(PointGreen, RoundedCornerShape(4.dp))
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${badge.currentValue} / ${badge.targetValue}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PointGreen
                    )
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) PointGreen else Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        "확인했어요!",
                        color = if (isCompleted) Color.White else TextMain,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
