package com.frontend.ui.screen.walk.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frontend.domain.model.NearbyDogResponse

private val WarningRed = Color(0xFFFF6B6B)

@Composable
fun DogWarningDialog(
    dog: NearbyDogResponse,
    onDismiss: () -> Unit,
) {
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
        // 다이얼로그 카드 — 터치 이벤트가 배경으로 전파되지 않도록 소비
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* 카드 터치는 배경 클릭 방지용 소비 — 닫지 않음 */ },
                )
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 빨간 원 안 경고 아이콘
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(WarningRed, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "경고",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "주의",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "서로 다른 성향의 친구가\n다가오고 있어요",
                fontSize = 15.sp,
                color = Color(0xFF555577),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "이름 : ${dog.name}",
                fontSize = 15.sp,
                color = Color(0xFF1A1A2E),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "견종 : ${dog.breed}",
                fontSize = 15.sp,
                color = Color(0xFF1A1A2E),
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "거리 : ${dog.distanceM.toInt()}m",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E),
            )
        }
    }
}
