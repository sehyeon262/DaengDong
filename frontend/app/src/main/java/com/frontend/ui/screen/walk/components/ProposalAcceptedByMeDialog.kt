package com.frontend.ui.screen.walk.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.frontend.ui.theme.PointGreen

@Composable
fun ProposalAcceptedByMeDialog(
    onDismiss: () -> Unit,
    chatRoomId: Long? = null,
    onStartChat: (Long) -> Unit = {},
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = PointGreen,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "수락 완료!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PointGreen
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "함께 산책하기를 수락했습니다 🐾\n상대방에게 수락 알림이 전송됐어요.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(22.dp))

                if (chatRoomId != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onStartChat(chatRoomId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PointGreen)
                    ) {
                        Text("채팅하기", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("확인", color = PointGreen, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
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
