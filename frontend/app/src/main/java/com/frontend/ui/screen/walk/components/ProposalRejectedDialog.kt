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
import com.frontend.domain.model.RejectedProposalInfo

private val rejectedFallbackDrawables = listOf(R.drawable.husky, R.drawable.poodle, R.drawable.french)

@Composable
fun ProposalRejectedDialog(
    rejected: RejectedProposalInfo,
    onDismiss: () -> Unit,
) {
    val fallback = rejectedFallbackDrawables[rejected.dogId.toInt() % rejectedFallbackDrawables.size]

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
                    model = rejected.profileImageUrl.takeIf { !it.isNullOrBlank() },
                    contentDescription = rejected.name,
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
                    text = "산책 제안 거절",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "${rejected.name}(${rejected.breed})이(가)\n산책 제안을 거절했어요 😢",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD))
                ) {
                    Text("확인", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
