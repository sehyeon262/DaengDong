package com.frontend.ui.screen.walk.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.frontend.R
import com.frontend.domain.model.DogProfileResponse
import com.frontend.domain.model.NearbyDogResponse
import com.frontend.ui.theme.PointGreen
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val dogFallbackDrawables = listOf(R.drawable.husky, R.drawable.poodle, R.drawable.french)

private val traitTagColors = listOf(
    Color(0xFFFFF9C4) to Color(0xFFF57F17),   // 노란색
    Color(0xFFE8F5E9) to Color(0xFF2E7D32),   // 초록색
    Color(0xFFE3F2FD) to Color(0xFF1565C0),   // 파란색
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyDogProfilePopup(
    nearbyDog: NearbyDogResponse,
    profile: DogProfileResponse?,
    isLoading: Boolean,
    proposalSent: Boolean = false,
    isSendingProposal: Boolean = false,
    chatRoomId: Long? = null,              // 수락된 채팅방 ID (있으면 채팅 버튼 표시)
    onDismiss: () -> Unit,
    onPropose: () -> Unit,
    onFeedback: (String) -> Unit = {},
    onStartChat: (Long) -> Unit = {},      // 채팅 시작하기 콜백
) {
    val sheetState = rememberModalBottomSheetState()
    val fallbackDrawable = dogFallbackDrawables[nearbyDog.dogId.toInt() % dogFallbackDrawables.size]

    // 버튼 상태 — API에서 받은 현재 피드백으로 초기화
    var selectedFeedback by remember(nearbyDog.dogId) {
        mutableStateOf(nearbyDog.feedback ?: "보통")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
    ) {
        if (isLoading || profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PointGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // ── 프로필 Row ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 프로필 이미지 (사진 있으면 사진, 없으면 견종 아이콘)
                    AsyncImage(
                        model = nearbyDog.profileImageUrl.takeIf { !it.isNullOrBlank() },
                        contentDescription = profile.name,
                        placeholder = painterResource(fallbackDrawable),
                        error = painterResource(fallbackDrawable),
                        fallback = painterResource(fallbackDrawable),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    )

                    Spacer(Modifier.width(14.dp))

                    // 이름 + 성별 + 견종/나이 + 거리
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (profile.gender == "MALE") "♂" else "♀",
                                fontSize = 18.sp,
                                color = if (profile.gender == "MALE") Color(0xFFE91E63) else Color(0xFF9C27B0)
                            )
                        }
                        Text(
                            text = "${profile.breed} · ${calcAge(profile.birthDate)}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = formatDistance(nearbyDog.distanceM),
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // 좋아요 / 싫어요 버튼
                    Row {
                        // 좋아요 버튼
                        IconButton(onClick = {
                            val next = if (selectedFeedback == "좋아요") "보통" else "좋아요"
                            selectedFeedback = next
                            onFeedback(next)
                        }) {
                            Icon(
                                imageVector = if (selectedFeedback == "좋아요")
                                    Icons.Default.Favorite
                                else
                                    Icons.Default.FavoriteBorder,
                                contentDescription = "좋아요",
                                tint = if (selectedFeedback == "좋아요")
                                    Color(0xFFFF5C8D)
                                else
                                    Color.LightGray,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        // 싫어요 버튼
                        IconButton(onClick = {
                            val next = if (selectedFeedback == "싫어요") "보통" else "싫어요"
                            selectedFeedback = next
                            onFeedback(next)
                        }) {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = "싫어요",
                                tint = if (selectedFeedback == "싫어요")
                                    Color(0xFFFF6B6B)
                                else
                                    Color.LightGray,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // ── 성향 태그 ───────────────────────────────────────────────
                val traits = profile.traits.orEmpty()
                if (traits.isNotEmpty()) {
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        traits.chunked(3).forEachIndexed { rowIdx, row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                row.forEachIndexed { colIdx, trait ->
                                    val (bg, fg) = traitTagColors[(rowIdx * 3 + colIdx) % traitTagColors.size]
                                    Box(
                                        modifier = Modifier
                                            .background(bg, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(text = "#$trait", fontSize = 13.sp, color = fg)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(16.dp))
                }

                // ── 채팅 시작하기 버튼 (수락 완료된 경우) ──────────────────
                if (chatRoomId != null) {
                    Button(
                        onClick = { onStartChat(chatRoomId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PointGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "채팅 시작하기",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    // ── 함께 산책 제안 버튼 ─────────────────────────────────────
                    Button(
                        onClick = { if (!proposalSent && !isSendingProposal) onPropose() },
                        enabled = !proposalSent && !isSendingProposal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (proposalSent) Color(0xFF9E9E9E) else PointGreen,
                            disabledContainerColor = if (proposalSent) Color(0xFF9E9E9E) else Color(0xFFBDBDBD),
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSendingProposal) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (proposalSent) "제안 전송됨" else "함께 산책 제안",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calcAge(birthDate: String): String {
    return try {
        val birth = LocalDate.parse(birthDate)
        val years = ChronoUnit.YEARS.between(birth, LocalDate.now()).toInt()
        if (years > 0) "${years}살" else "1살 미만"
    } catch (e: Exception) {
        ""
    }
}

private fun formatDistance(distanceM: Double): String {
    val meters = distanceM.toInt()
    val minutes = (distanceM / 80).toInt().coerceAtLeast(1)
    return "약 ${meters}m (${minutes}분)"
}
