package com.frontend.ui.screen.walk.components

import androidx.compose.foundation.layout.Arrangement
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
import com.frontend.domain.model.PendingProposalInfo
import com.frontend.ui.theme.PointGreen

private val proposalFallbackDrawables = listOf(R.drawable.husky, R.drawable.poodle, R.drawable.french)

@Composable
fun ProposalIncomingDialog(
    proposal: PendingProposalInfo,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val fallback = proposalFallbackDrawables[proposal.dogId.toInt() % proposalFallbackDrawables.size]

    Dialog(onDismissRequest = onReject) {
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
                    model = proposal.profileImageUrl.takeIf { !it.isNullOrBlank() },
                    contentDescription = proposal.name,
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
                    text = "함께 산책 제안이 왔어요!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "${proposal.name}(${proposal.breed})이(가)\n함께 산책하고 싶어해요 🐾",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("거절", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PointGreen)
                    ) {
                        Text("수락", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
