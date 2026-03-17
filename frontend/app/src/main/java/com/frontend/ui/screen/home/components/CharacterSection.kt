package com.frontend.ui.screen.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.frontend.R
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextMain
import com.frontend.util.getCharacterImage
import com.frontend.util.getWalkStatusColor
import com.frontend.util.getWalkStatusLabel

@Composable
fun CharacterSection(
    characterType: String,
    walkStatus: String,
    walkMessage: String
) {
    val characterRes = getCharacterImage(characterType)
    val statusColor = getWalkStatusColor(walkStatus)
    val statusLabel = getWalkStatusLabel(walkStatus)
    val showClouds = walkStatus == "GOOD"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            // 강아지 캐릭터 - 꽉 채우기
            Image(
                painter = painterResource(id = characterRes),
                contentDescription = "캐릭터",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // GOOD일 때 구름 - 강아지 귀 옆에 아기자기하게
            if (showClouds) {
                // 왼쪽 구름 - 강아지 머리 바로 왼쪽
                Image(
                    painter = painterResource(id = R.drawable.cloud),
                    contentDescription = "구름",
                    modifier = Modifier
                        .size(65.dp)
                        .align(Alignment.TopStart)
                        .offset(x = 4.dp, y = 48.dp),
                    contentScale = ContentScale.Fit
                )
                // 오른쪽 구름 - 강아지 머리 바로 오른쪽
                Image(
                    painter = painterResource(id = R.drawable.cloud),
                    contentDescription = "구름",
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-28).dp, y = 36.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 흰색 카드 + 연두색 테두리
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, PointGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 바깥 연한 원 + 안쪽 색깔 원 뱃지
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                ) {
                    Text(
                        text = statusLabel,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Text(
                text = walkMessage.replace(". ", ".\n"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextMain,
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
