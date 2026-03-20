package com.frontend.ui.screen.dog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.frontend.R
import com.frontend.domain.model.DogProfileResponse
import com.frontend.navigation.Routes
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import java.util.Calendar
import androidx.compose.ui.platform.LocalLifecycleOwner

private val TRAIT_COLORS = listOf(
    Color(0xFFB2EBE9) to Color(0xFF2E7D7B),
    Color(0xFFFFF9C4) to Color(0xFF7B6D00),
    Color(0xFFF0F0F0) to Color(0xFF555555),
    Color(0xFFFFCDD2) to Color(0xFF9B2226),
    Color(0xFFD1C4E9) to Color(0xFF4527A0),
    Color(0xFFFFE0B2) to Color(0xFF8B4513),
)
private const val MOCK_WALK_MINUTES = 45
private const val MOCK_WALK_KM = 1.2
private val BADGE_COLOR_SETS = listOf(
    listOf(Color(0xFFFF8A65), Color(0xFFFFB74D), Color(0xFF81C784)),
    listOf(Color(0xFF4DB6AC), Color(0xFFAED581), Color(0xFF7986CB)),
    listOf(Color(0xFF64B5F6), Color(0xFF4DD0E1), Color(0xFF9575CD)),
)

private fun calculateAge(birthDateStr: String): Int {
    return try {
        val parts = birthDateStr.split("-")
        val birthYear = parts[0].toInt()
        val birthMonth = parts[1].toInt()
        val birthDay = parts[2].toInt()
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - birthYear
        val m = today.get(Calendar.MONTH) + 1
        val d = today.get(Calendar.DAY_OF_MONTH)
        if (m < birthMonth || (m == birthMonth && d < birthDay)) age--
        age
    } catch (e: Exception) { 0 }
}

@Composable
fun DogProfileScreen(
    navController: NavController,
    viewModel: DogProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDogProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF6EE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("반려견 프로필", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain)
            IconButton(onClick = { navController.navigate(Routes.DOG_EDIT) }) {
                Icon(Icons.Filled.Edit, contentDescription = "수정", tint = PointGreen)
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PointGreen)
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("프로필을 불러올 수 없습니다", color = TextGray, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(state.error!!, color = TextGray, fontSize = 12.sp)
                }
            }
            state.profile != null -> DogProfileContent(
                profile = state.profile!!,
                recentMetDog = state.recentMetDog,
                onMetDogsClick = {
                    state.dogId?.let { dogId ->
                        navController.navigate(Routes.metDogs(dogId))
                    }
                }
            )
        }
    }
}

@Composable
private fun DogProfileContent(
    profile: DogProfileResponse,
    recentMetDog: com.frontend.domain.model.MetDogResponse?,
    onMetDogsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ProfileSection(profile) }
        item { TraitsSection(profile.traits.orEmpty()) }
        item { WalkStatsSection() }
        item { RecentFriendSection(recentMetDog, onMetDogsClick) }
        item { BadgeSection() }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProfileSection(profile: DogProfileResponse) {
    val age = calculateAge(profile.birthDate)
    val weightStr = if (profile.weight == profile.weight.toLong().toDouble())
        "${profile.weight.toInt()}kg" else "${profile.weight}kg"
    val genderSymbol = if (profile.gender == "FEMALE") "♀" else "♂"
    val genderColor = if (profile.gender == "FEMALE") Color(0xFF888888) else Color(0xFF5585BB)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(90.dp)) {
                AsyncImage(
                    model = R.drawable.default_profile,
                    contentDescription = "강아지 사진",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(2.dp, PointGreen.copy(alpha = 0.5f), CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.default_profile),
                    placeholder = painterResource(R.drawable.default_profile)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .background(Color(0xFFF5B73D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Pets, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(Modifier.width(6.dp))
                    Text(genderSymbol, fontSize = 20.sp, color = genderColor, fontWeight = FontWeight.Medium)
                }
                Text("${age}세, $weightStr", fontSize = 14.sp, color = TextGray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(profile.breed, Color(0xFFD5ECC2), Color(0xFF4A6E3A))
                    InfoChip("중성화", Color(0xFFFFF0B3), Color(0xFF8B6E00))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("우리 강아지 프로필을 완성하고 산책을 시작해요 \uD83D\uDC3E", fontSize = 13.sp, color = TextGray)
    }
}

@Composable
private fun TraitsSection(traits: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("성향", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
        if (traits.isEmpty()) {
            Text("등록된 성향 태그가 없습니다", fontSize = 13.sp, color = TextGray)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                traits.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { i, trait ->
                            val idx = (traits.indexOf(trait) + i).coerceIn(0, TRAIT_COLORS.size - 1)
                            val (bg, fg) = TRAIT_COLORS[idx]
                            TraitChip(trait, bg, fg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalkStatsSection() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WalkStatCard(
            modifier = Modifier.weight(1f), title = "평균 산책 시간",
            icon = { Icon(Icons.Filled.HourglassEmpty, null, tint = Color(0xFF6B5A00), modifier = Modifier.size(32.dp)) },
            value = "${MOCK_WALK_MINUTES}분"
        )
        WalkStatCard(
            modifier = Modifier.weight(1f), title = "평균 산책 거리",
            icon = { Icon(Icons.Filled.Shuffle, null, tint = Color(0xFF6B5A00), modifier = Modifier.size(32.dp)) },
            value = "${MOCK_WALK_KM}km"
        )
    }
}

@Composable
private fun WalkStatCard(modifier: Modifier, title: String, icon: @Composable () -> Unit, value: String) {
    Box(
        modifier = modifier
            .border(1.5.dp, PointGreen.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 12.sp, color = TextGray)
            Box(
                modifier = Modifier.size(52.dp).background(Color(0xFFFFF0B3), CircleShape),
                contentAlignment = Alignment.Center
            ) { icon() }
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
        }
    }
}

@Composable
private fun RecentFriendSection(
    recentMetDog: com.frontend.domain.model.MetDogResponse?,
    onMetDogsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMetDogsClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("최근 만난 친구", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text("더보기", fontSize = 13.sp, color = TextGray)
        }
        if (recentMetDog == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMetDogsClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("아직 만난 친구가 없어요", fontSize = 14.sp, color = TextGray)
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMetDogsClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = recentMetDog.targetDogProfileImageUrl,
                            contentDescription = "${recentMetDog.targetDogName} 프로필",
                            modifier = Modifier.size(52.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.default_profile),
                            error = painterResource(R.drawable.default_profile)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(recentMetDog.targetDogName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                                if (recentMetDog.targetDogBreed != null) {
                                    InfoChip(recentMetDog.targetDogBreed, Color(0xFFF2EFE8), Color(0xFF7A6A50))
                                }
                            }
                            Text(formatLastMetDate(recentMetDog.lastMetAt), fontSize = 13.sp, color = TextGray)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (!recentMetDog.feedbackDone) {
                            Text("궁합 평가하기", fontSize = 14.sp, color = TextGray)
                        } else {
                            when (recentMetDog.feedback) {
                                "좋아요" -> Box(
                                    modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
                                ) { Text("잘 맞아요", fontSize = 13.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold) }
                                "싫어요" -> Box(
                                    modifier = Modifier.background(Color(0xFFFFEBEE), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
                                ) { Text("안 맞아요", fontSize = 13.sp, color = Color(0xFFF44336), fontWeight = FontWeight.SemiBold) }
                                else -> Text("평가 완료", fontSize = 14.sp, color = TextGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatLastMetDate(dateTimeStr: String): String {
    return try {
        val dateTime = java.time.LocalDateTime.parse(dateTimeStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd에 만남")
        dateTime.format(formatter)
    } catch (e: Exception) {
        dateTimeStr
    }
}

@Composable
private fun BadgeSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("나의 뱃지", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(BADGE_COLOR_SETS) { colors ->
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Brush.radialGradient(colors), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(text, fontSize = 12.sp, color = fg, fontWeight = FontWeight.Medium) }
}

@Composable
private fun TraitChip(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) { Text(text, fontSize = 14.sp, color = fg) }
}
