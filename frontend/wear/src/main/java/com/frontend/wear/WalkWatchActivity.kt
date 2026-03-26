package com.frontend.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

class WalkWatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WalkWatchScreen()
        }
    }
}

@Composable
fun WalkWatchScreen() {
    val stats by WalkStatsHolder.stats.collectAsState()

    val hours   = stats.elapsedSeconds / 3600
    val minutes = (stats.elapsedSeconds % 3600) / 60
    val seconds = stats.elapsedSeconds % 60
    val timeText = if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
    val distKm = stats.distanceMeters / 1000.0

    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!stats.isWalking) {
                // 산책 전 대기 화면
                Text(
                    text = "🐾",
                    fontSize = 32.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "산책을 시작해주세요",
                    style = MaterialTheme.typography.body1,
                    color = Color.Gray,
                )
            } else {
                // 산책 중 통계 화면
                val timeColor = if (stats.isPaused) Color.Gray else Color.White

                // 경과 시간 (크게)
                Text(
                    text = timeText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = timeColor,
                )
                Spacer(Modifier.height(6.dp))

                // 거리
                Text(
                    text = "%.2f km".format(distKm),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF81C784), // 초록
                )
                Spacer(Modifier.height(4.dp))

                // 칼로리
                Text(
                    text = "${stats.calories} kcal",
                    fontSize = 14.sp,
                    color = Color(0xFFFFB74D), // 주황
                )

                // 일시정지 표시
                if (stats.isPaused) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "⏸ 일시정지",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}
