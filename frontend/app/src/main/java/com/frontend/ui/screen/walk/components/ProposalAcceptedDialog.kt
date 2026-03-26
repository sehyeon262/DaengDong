package com.frontend.ui.screen.walk.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.frontend.R
import com.frontend.domain.model.AcceptedProposalInfo
import com.frontend.ui.theme.PointGreen

private val acceptedFallbackDrawables = listOf(R.drawable.husky, R.drawable.poodle, R.drawable.french)

@Composable
fun ProposalAcceptedDialog(
    accepted: AcceptedProposalInfo,
    onDismiss: () -> Unit,
    onStartChat: (Long) -> Unit = {},
) {
    val fallback = acceptedFallbackDrawables[accepted.dogId.toInt() % acceptedFallbackDrawables.size]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 프로필 이미지
                AsyncImage(
                    model = accepted.profileImageUrl.takeIf { !it.isNullOrBlank() },
                    contentDescription = accepted.name,
                    placeholder = painterResource(fallback),
                    error = painterResource(fallback),
                    fallback = painterResource(fallback),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "함께 산책 수락!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PointGreen
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "${accepted.name}(${accepted.breed})이(가)\n함께 산책을 수락했어요! 🎉",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(20.dp))

                // 채팅방이 있으면 '채팅하기' 버튼도 표시
                if (accepted.chatRoomId != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onStartChat(accepted.chatRoomId)
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PointGreen)
                    ) {
                        Text("채팅하기", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("확인", color = PointGreen, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PointGreen)
                    ) {
                        Text("확인", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
