package com.frontend.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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
    val courses by CoursesHolder.courses.collectAsState()
    var showCourseSelection by remember { mutableStateOf(false) }
    var walkResult by remember { mutableStateOf<Triple<Int, Double, Int>?>(null) }

    // 산책 시작 확인 시 코스 선택 화면 닫기 (Bluetooth 응답 후 전환)
    LaunchedEffect(stats.isWalking) {
        if (stats.isWalking) showCourseSelection = false
    }

    // 로컬 타이머: Bluetooth 공백에도 1초씩 카운트
    var localElapsed by remember { mutableStateOf(stats.elapsedSeconds) }
    LaunchedEffect(stats.elapsedSeconds) { localElapsed = stats.elapsedSeconds }
    LaunchedEffect(stats.isWalking, stats.isPaused) {
        if (stats.isWalking && !stats.isPaused) {
            while (true) { delay(1000L); localElapsed++ }
        }
    }

    val proposal by ProposalHolder.proposal.collectAsState()
    val footprint by FootprintHolder.footprint.collectAsState()
    val badge by BadgeHolder.badge.collectAsState()
    val dogWarning by DogWarningHolder.warning.collectAsState()
    val dangerZone by DangerZoneHolder.danger.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sendAction: (String, JSONObject?) -> Unit = { path, json ->
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                android.util.Log.d("WalkWatch", "연결된 노드: ${nodes.map { "${it.id}(${it.displayName})" }}")
                val nodeId = nodes.firstOrNull()?.id
                if (nodeId == null) {
                    android.util.Log.e("WalkWatch", "sendAction 실패: 연결된 폰 없음, path=$path")
                    return@launch
                }
                val data = json?.toString()?.toByteArray() ?: ByteArray(0)
                Wearable.getMessageClient(context).sendMessage(nodeId, path, data).await()
                android.util.Log.d("WalkWatch", "sendAction 성공: path=$path → nodeId=$nodeId")
            } catch (e: Exception) {
                android.util.Log.e("WalkWatch", "sendMessage 실패: path=$path, error=${e.message}", e)
            }
        }
    }

    val hours = localElapsed / 3600
    val minutes = (localElapsed % 3600) / 60
    val seconds = localElapsed % 60
    val timeText = "%02d:%02d:%02d".format(hours, minutes, seconds)
    val distKm = stats.distanceMeters / 1000.0

    Scaffold(timeText = { TimeText() }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            // ── 메인 화면 ───────────────────────────────────────────
            if (showCourseSelection && !stats.isWalking) {
                // 코스 선택 화면
                val coursePagerState = rememberPagerState(pageCount = { courses.size })
                val selectedCourse = courses.getOrNull(coursePagerState.currentPage)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text("코스 선택", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                    Spacer(Modifier.height(4.dp))
                    HorizontalPager(
                        state = coursePagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) { page ->
                        val course = courses[page]
                        val icon = when (course.type) {
                            "FREE" -> "🚶"; "SHORT" -> "⚡"; "RECOMMENDED" -> "💡"; "EXPLORE" -> "🗺️"; else -> "🐾"
                        }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.size(90.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(icon, fontSize = 28.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(course.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                if (course.type != "FREE") {
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("%.2f".format(course.distanceKm), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("km", fontSize = 10.sp, color = Color(0xFF888888))
                                        }
                                        Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(20.dp).background(Color(0xFF444444)))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${course.durationMin}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("min", fontSize = 10.sp, color = Color(0xFF888888))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            selectedCourse?.let { c ->
                                sendAction("/action/select_course", JSONObject().apply { put("courseIndex", c.index) })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF5B9E5F)),
                        modifier = Modifier.padding(bottom = 8.dp).size(width = 100.dp, height = 32.dp),
                    ) { Text("선택", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            } else if (walkResult != null && !stats.isWalking) {
                // 산책 결과 화면
                val (rSec, rDist, rCal) = walkResult!!
                val rH = rSec / 3600; val rM = (rSec % 3600) / 60; val rS = rSec % 60
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("소요 시간", fontSize = 13.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Medium)
                            Text("%02d:%02d:%02d".format(rH, rM, rS), fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("거리", fontSize = 13.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Medium)
                            Text("%.2f km".format(rDist / 1000.0), fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("칼로리", fontSize = 13.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Medium)
                            Text("$rCal kcal", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                walkResult = null
                                sendAction("/action/dismiss_summary", null)
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF5B9E5F)),
                            modifier = Modifier.size(width = 100.dp, height = 36.dp),
                        ) { Text("확인", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            } else if (!stats.isWalking) {
                // 산책 전 대기 화면
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF5B9E5F).copy(alpha = 0.35f),
                                        Color(0xFF5B9E5F).copy(alpha = 0.10f),
                                        Color.Transparent,
                                    ),
                                ),
                                shape = CircleShape,
                            ),
                    )
                    Button(
                        onClick = { showCourseSelection = true },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF5B9E5F)),
                        modifier = Modifier.size(140.dp),
                        shape = CircleShape,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.face),
                                contentDescription = "강아지",
                                modifier = Modifier.size(72.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "산책 시작",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            } else {
                // 산책 중 — HorizontalPager
                val pagerState = rememberPagerState(pageCount = { 2 })
                val timeColor = if (stats.isPaused) Color(0xFF888888) else Color.White

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = if (stats.isPaused) "일시정지" else "산책 중",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (stats.isPaused) Color(0xFF888888) else Color(0xFF76C442),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = timeText,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = timeColor,
                                    letterSpacing = (-0.5).sp,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("%.2f".format(distKm), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("km", fontSize = 11.sp, color = Color(0xFF888888))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 14.dp)
                                            .width(1.dp)
                                            .height(30.dp)
                                            .background(Color(0xFF444444)),
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${stats.calories}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("kcal", fontSize = 11.sp, color = Color(0xFF888888))
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp),
                            ) {
                                Box(Modifier.size(5.dp).background(Color.White, CircleShape))
                                Box(Modifier.size(5.dp).background(Color(0xFF444444), CircleShape))
                            }
                        }

                        1 -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (stats.isPaused) {
                                    Button(
                                        onClick = { sendAction("/action/resume_walk", null) },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                                        modifier = Modifier.size(width = 130.dp, height = 48.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("▶", fontSize = 10.sp, color = Color.White)
                                            Spacer(Modifier.width(6.dp))
                                            Text("재개", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { sendAction("/action/pause_walk", null) },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF555555)),
                                        modifier = Modifier.size(width = 130.dp, height = 48.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("▐▐", fontSize = 10.sp, color = Color.White)
                                            Spacer(Modifier.width(6.dp))
                                            Text("일시정지", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                    walkResult = Triple(localElapsed, stats.distanceMeters, stats.calories)
                                    sendAction("/action/end_walk", null)
                                },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935)),
                                    modifier = Modifier.size(width = 130.dp, height = 48.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("■", fontSize = 14.sp, color = Color.White)
                                        Spacer(Modifier.width(6.dp))
                                        Text("산책 종료", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp),
                            ) {
                                Box(Modifier.size(5.dp).background(Color(0xFF444444), CircleShape))
                                Box(Modifier.size(5.dp).background(Color.White, CircleShape))
                            }
                        }
                    }
                }
            }

            // ── 오버레이 (우선순위 순) ──────────────────────────────
            if (dogWarning.isVisible) {
                DogWarningOverlay(
                    dogName = dogWarning.dogName,
                    distance = dogWarning.distance,
                    onDismiss = { DogWarningHolder.dismiss() },
                )
            }
            if (dangerZone.isVisible) {
                DangerZoneOverlay(
                    reason = dangerZone.reason,
                    onDismiss = { DangerZoneHolder.dismiss() },
                )
            }
            if (badge.isVisible) {
                BadgeOverlay(
                    badgeId = badge.badgeId,
                    badgeName = badge.badgeName,
                    onDismiss = { BadgeHolder.dismiss() },
                )
            }
            if (footprint.isVisible) {
                FootprintOverlay(
                    placeName = footprint.placeName,
                    onStamp = {
                        sendAction("/response/stamp", JSONObject().apply {
                            put("walkId", footprint.walkId)
                            put("dogId", footprint.dogId)
                            put("placeId", footprint.placeId)
                        })
                        // dismiss는 성공 애니메이션 후 onDismiss에서 처리
                    },
                    onDismiss = { FootprintHolder.dismiss() },
                )
            }
            if (proposal.isVisible) {
                ProposalOverlay(
                    dogName = proposal.dogName,
                    breed = proposal.breed,
                    onAccept = {
                        sendAction("/response/proposal_accept", JSONObject().apply {
                            put("proposalId", proposal.proposalId)
                            put("myWalkRecordId", proposal.myWalkRecordId)
                        })
                        ProposalHolder.dismiss()
                    },
                    onReject = {
                        sendAction("/response/proposal_reject", JSONObject().apply {
                            put("proposalId", proposal.proposalId)
                            put("myWalkRecordId", proposal.myWalkRecordId)
                        })
                        ProposalHolder.dismiss()
                    },
                )
            }
        }
    }
}

// ── 공통: 강아지 얼굴 + 글로우 ─────────────────────────────────────
@Composable
private fun DogFaceWithGlow(glowColor: Color = Color.Transparent, blinking: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )
    val glowAlpha = if (blinking) blinkAlpha else 0.8f

    Box(contentAlignment = Alignment.Center) {
        if (glowColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = glowAlpha),
                                glowColor.copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
        }
        Image(
            painter = painterResource(id = R.drawable.face),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
        )
    }
}

// ── 비선호 강아지 알림 오버레이 ────────────────────────────────────
@Composable
private fun DogWarningOverlay(dogName: String, distance: Int, onDismiss: () -> Unit) {
    var showText by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // 진동
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1))
        // 2초 후 텍스트 표시
        kotlinx.coroutines.delay(2000)
        showText = true
        // 5초 후 자동 닫기
        kotlinx.coroutines.delay(5000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DogFaceWithGlow(glowColor = Color(0xFFFF1010), blinking = !showText)
            if (showText) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$dogName 접근 알림",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF1010),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${distance}m 내에 있어요!",
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── 위험 구역 알림 오버레이 ────────────────────────────────────────
@Composable
private fun DangerZoneOverlay(reason: String, onDismiss: () -> Unit) {
    var showText by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "danger_glow")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "danger_alpha",
    )
    val glowAlpha = if (!showText) blinkAlpha else 0.8f

    LaunchedEffect(Unit) {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1))
        kotlinx.coroutines.delay(2000)
        showText = true
        kotlinx.coroutines.delay(5000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF1010).copy(alpha = glowAlpha),
                                    Color(0xFFFF1010).copy(alpha = glowAlpha * 0.5f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        ),
                )
                Image(
                    painter = painterResource(id = R.drawable.danger_flag),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                )
            }
            if (showText) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "위험 구역 알림",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF1010),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "위험 구역에\n가까워졌어요!",
                    fontSize = 13.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun badgeImageRes(badgeId: Long): Int = when (badgeId) {
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

// ── 배지 획득 오버레이 (폭죽 애니메이션) ──────────────────────────
@Composable
private fun BadgeOverlay(badgeId: Long, badgeName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        kotlinx.coroutines.delay(5000)
        onDismiss()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    // 두 개의 웨이브 — 교차 터짐
    val progress1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "confetti_p1",
    )
    val progress2 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "confetti_p2",
    )

    // 파티클: (각도, 거리비율, 색, 크기)
    data class Particle(val angle: Float, val dist: Float, val color: Color, val radius: Float)
    val vividColors = listOf(
        Color(0xFFFF3B3B), Color(0xFFFF9500), Color(0xFFFFD700),
        Color(0xFF4CD964), Color(0xFF34AADC), Color(0xFFFF2D55),
        Color(0xFFAF52DE), Color(0xFFFFFFFF),
    )
    val particles = remember {
        List(40) {
            Particle(
                angle = Random.nextFloat() * 360f,
                dist = Random.nextFloat() * 0.4f + 0.3f,
                color = vividColors[it % vividColors.size],
                radius = Random.nextFloat() * 4f + 4f, // 4~8f
            )
        }
    }

    fun drawWave(scope: androidx.compose.ui.graphics.drawscope.DrawScope, p: Float) {
        val prog = p % 1f
        val cx = scope.size.width / 2
        val cy = scope.size.height / 2
        val maxR = scope.size.minDimension * 0.48f
        particles.forEach { particle ->
            val rad = Math.toRadians(particle.angle.toDouble())
            val r = maxR * particle.dist * prog
            val alpha = (1f - prog).coerceIn(0f, 1f)
            scope.drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.radius,
                center = Offset(cx + (cos(rad) * r).toFloat(), cy + (sin(rad) * r).toFloat()),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // 폭죽 파티클 (웨이브 2개)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawWave(this, progress1)
            drawWave(this, progress2)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.5f),
                                    Color(0xFFFFD700).copy(alpha = 0.2f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        ),
                )
                Image(
                    painter = painterResource(id = badgeImageRes(badgeId)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "배지 획득!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = badgeName,
                fontSize = 13.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── 발자국 오버레이 ────────────────────────────────────────────────
@Composable
private fun FootprintOverlay(placeName: String, onStamp: () -> Unit, onDismiss: () -> Unit) {
    var stamped by remember { mutableStateOf(false) }

    // 발자국 남긴 후 3초 뒤 자동 닫기
    LaunchedEffect(stamped) {
        if (stamped) {
            kotlinx.coroutines.delay(3000)
            onDismiss()
        }
    }

    // 폭죽 애니메이션 (stamped 후 표시)
    val infiniteTransition = rememberInfiniteTransition(label = "fp_confetti")
    val progress1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "fp_p1",
    )
    val progress2 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "fp_p2",
    )
    val vividColors = listOf(
        Color(0xFFFF3B3B), Color(0xFFFF9500), Color(0xFFFFD700),
        Color(0xFF4CD964), Color(0xFF34AADC), Color(0xFFFF2D55),
        Color(0xFFAF52DE), Color(0xFFFFFFFF),
    )
    data class Particle(val angle: Float, val dist: Float, val color: Color, val radius: Float)
    val particles = remember {
        List(40) {
            Particle(
                angle = Random.nextFloat() * 360f,
                dist = Random.nextFloat() * 0.4f + 0.3f,
                color = vividColors[it % vividColors.size],
                radius = Random.nextFloat() * 4f + 4f,
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (stamped) {
            // 폭죽
            Canvas(modifier = Modifier.fillMaxSize()) {
                listOf(progress1, progress2).forEach { p ->
                    val prog = p % 1f
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val maxR = size.minDimension * 0.48f
                    particles.forEach { particle ->
                        val rad = Math.toRadians(particle.angle.toDouble())
                        val r = maxR * particle.dist * prog
                        val alpha = (1f - prog).coerceIn(0f, 1f)
                        drawCircle(
                            color = particle.color.copy(alpha = alpha),
                            radius = particle.radius,
                            center = Offset(cx + (cos(rad) * r).toFloat(), cy + (sin(rad) * r).toFloat()),
                        )
                    }
                }
            }
            // 성공 화면
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.4f),
                                        Color(0xFFFFD700).copy(alpha = 0.15f),
                                        Color.Transparent,
                                    ),
                                ),
                                shape = CircleShape,
                            ),
                    )
                    Image(
                        painter = painterResource(id = R.drawable.place_mark),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "발자국을 남겼어요!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = placeName,
                    fontSize = 12.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // 기본 화면
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                DogFaceWithGlow()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "발자국",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = placeName,
                    fontSize = 12.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onStamp(); stamped = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE8E8E8)),
                    modifier = Modifier.size(width = 120.dp, height = 36.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("발자국 남기기", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── 산책 제안 오버레이 ─────────────────────────────────────────────
@Composable
private fun ProposalOverlay(
    dogName: String,
    breed: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    var showContent by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "proposal_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "proposal_glow_alpha",
    )
    val glowSize by infiniteTransition.animateFloat(
        initialValue = 95f, targetValue = 115f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "proposal_glow_size",
    )

    LaunchedEffect(Unit) {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
        kotlinx.coroutines.delay(1500)
        showContent = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            if (!showContent) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(glowSize.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF4CAF50).copy(alpha = glowAlpha),
                                        Color(0xFF4CAF50).copy(alpha = glowAlpha * 0.3f),
                                        Color.Transparent,
                                    ),
                                ),
                                shape = CircleShape,
                            ),
                    )
                    Image(
                        painter = painterResource(id = R.drawable.face),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                    )
                }
            }
            if (showContent) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "함께 산책해요!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "$dogName($breed) 보호자님",
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                    modifier = Modifier.size(width = 130.dp, height = 40.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text("수락", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333)),
                    modifier = Modifier.size(width = 130.dp, height = 40.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text("거절", fontSize = 15.sp, color = Color(0xFF888888), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
