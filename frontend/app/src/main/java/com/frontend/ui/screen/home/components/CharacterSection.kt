package com.frontend.ui.screen.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
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
    walkMessage: String,
    temperature: Int = 20,
    fineDustGrade: String = ""
) {
    val characterRes = getCharacterImage(characterType, temperature, fineDustGrade)
    val statusColor = getWalkStatusColor(walkStatus)
    val statusLabel = getWalkStatusLabel(walkStatus)
    val sky = characterType.split("_").getOrNull(0) ?: ""
    val showClouds = walkStatus == "GOOD"
    val showBest = walkStatus == "GREAT"
    val showSun = sky == "SUNNY" && walkStatus == "CAUTION" && temperature >= 28
    val showRain = sky in listOf("RAINY", "SNOWY")
    val showDust = characterRes == R.drawable.dust

    // 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "decoration")
    val leftOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leftAnim"
    )
    val rightOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rightAnim"
    )
    // sun/rain용 위아래 애니메이션
    val sunOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunAnim"
    )
    val rainLeftOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rainLeft"
    )
    val rainRightOffset by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rainRight"
    )
    // dust 양쪽 둥둥 애니메이션
    val dustLeftOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dustLeft"
    )
    val dustRightOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dustRight"
    )
    val dustAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dustAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            // 강아지 캐릭터
            Image(
                painter = painterResource(id = characterRes),
                contentDescription = "캐릭터",
                modifier = if (showRain) {
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp, vertical = 16.dp)
                } else {
                    Modifier.fillMaxSize()
                },
                contentScale = ContentScale.Fit
            )

            // GOOD - 구름
            if (showClouds) {
                Image(
                    painter = painterResource(id = R.drawable.cloud),
                    contentDescription = "구름",
                    modifier = Modifier
                        .size(65.dp)
                        .align(Alignment.TopStart)
                        .offset(x = 4.dp, y = (48 + leftOffset).dp),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.cloud),
                    contentDescription = "구름",
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-28).dp, y = (36 + rightOffset).dp),
                    contentScale = ContentScale.Fit
                )
            }

            // BEST - best_left / best_rignt 양쪽
            if (showBest) {
                Image(
                    painter = painterResource(id = R.drawable.best_left),
                    contentDescription = "베스트 왼쪽",
                    modifier = Modifier
                        .size(70.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = 4.dp, y = leftOffset.dp),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.best_rignt),
                    contentDescription = "베스트 오른쪽",
                    modifier = Modifier
                        .size(70.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-4).dp, y = rightOffset.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // HOT - 태양 (왼쪽 상단, 크게)
            if (showSun) {
                Image(
                    painter = painterResource(id = R.drawable.sun),
                    contentDescription = "태양",
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.TopStart)
                        .offset(x = 8.dp, y = (4 + sunOffset).dp),
                    contentScale = ContentScale.Fit
                )
            }

            // RAINY - 비 양쪽
            if (showRain) {
                Image(
                    painter = painterResource(id = R.drawable.rain),
                    contentDescription = "비 왼쪽",
                    modifier = Modifier
                        .size(38.dp)
                        .align(Alignment.TopStart)
                        .offset(x = 12.dp, y = (10 + rainLeftOffset).dp),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.rain),
                    contentDescription = "비 오른쪽",
                    modifier = Modifier
                        .size(38.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-12).dp, y = (18 + rainRightOffset).dp),
                    contentScale = ContentScale.Fit
                )
            }

            // DUST - 먼지 아이콘 양쪽
            if (showDust) {
                Image(
                    painter = painterResource(id = R.drawable.dust_icon),
                    contentDescription = "먼지 왼쪽",
                    alpha = dustAlpha,
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = 2.dp, y = (dustLeftOffset).dp),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.dust_icon),
                    contentDescription = "먼지 오른쪽",
                    alpha = dustAlpha,
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-4).dp, y = (dustRightOffset).dp),
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
