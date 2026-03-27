package com.frontend.ui.screen.dog

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.frontend.ui.theme.Background
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import java.util.Calendar
import androidx.lifecycle.compose.LocalLifecycleOwner

private val TRAIT_COLORS = listOf(
    Color(0xFFE8F5E9) to Color(0xFF2E7D32),
    Color(0xFFFFF8E1) to Color(0xFF8D6E00),
    Color(0xFFF3E5F5) to Color(0xFF6A1B9A),
    Color(0xFFE3F2FD) to Color(0xFF1565C0),
    Color(0xFFFCE4EC) to Color(0xFFC62828),
    Color(0xFFFFF3E0) to Color(0xFFBF360C),
)
private const val MOCK_WALK_MINUTES = 45
private const val MOCK_WALK_KM = 1.2

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

    when {
        state.isLoading -> Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PointGreen)
        }
        state.error != null -> Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("프로필을 불러올 수 없습니다", color = TextGray, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(state.error!!, color = TextGray, fontSize = 12.sp)
            }
        }
        state.profile != null -> DogProfileContent(
            profile = state.profile!!,
            recentMetDog = state.recentMetDog,
            earnedBadges = state.earnedBadges,
            onEditClick = { navController.navigate(Routes.DOG_EDIT) },
            onMetDogsClick = {
                state.dogId?.let { dogId ->
                    navController.navigate(Routes.metDogs(dogId))
                }
            },
            onBadgesClick = {
                navController.navigate(Routes.BADGES)
            },
            onLogoutClick = {
                viewModel.logout {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        )
    }
}

@Composable
private fun DogProfileContent(
    profile: DogProfileResponse,
    recentMetDog: com.frontend.domain.model.MetDogResponse?,
    earnedBadges: List<com.frontend.domain.model.BadgeProgressResponse>,
    onEditClick: () -> Unit,
    onMetDogsClick: () -> Unit,
    onBadgesClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { ProfileHeroSection(profile, onEditClick) }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                WalkStatsSection()
                RecentFriendSection(recentMetDog, onMetDogsClick)
                BadgeSection(earnedBadges, onBadgesClick)
            }
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE53935)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935))
            ) {
                Text("로그아웃", fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ProfileHeroSection(profile: DogProfileResponse, onEditClick: () -> Unit) {
    val age = calculateAge(profile.birthDate)
    val weightStr = if (profile.weight == profile.weight.toLong().toDouble())
        "${profile.weight.toInt()}kg" else "${profile.weight}kg"
    val genderSymbol = if (profile.gender == "FEMALE") "♀" else "♂"
    val genderColor = if (profile.gender == "FEMALE") Color(0xFFE91E63) else Color(0xFF42A5F5)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PointGreen.copy(alpha = 0.15f),
                        Background
                    )
                )
            )
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        // Edit button top-right
        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "수정", tint = PointGreen)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Profile image - larger, centered
            Box(modifier = Modifier.size(110.dp)) {
                AsyncImage(
                    model = R.drawable.default_profile,
                    contentDescription = "강아지 사진",
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .border(3.dp, PointGreen.copy(alpha = 0.4f), CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.default_profile),
                    placeholder = painterResource(R.drawable.default_profile)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(30.dp)
                        .background(PointGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Pets, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            // Name + gender
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    genderSymbol,
                    fontSize = 22.sp,
                    color = genderColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(6.dp))

            // Breed + age + weight in a single line
            Text(
                "${profile.breed}  ·  ${age}세  ·  $weightStr",
                fontSize = 14.sp,
                color = TextGray
            )

            // Traits tags
            val traits = profile.traits.orEmpty()
            if (traits.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    traits.take(6).forEachIndexed { idx, trait ->
                        val colorIdx = idx.coerceIn(0, TRAIT_COLORS.size - 1)
                        val (bg, fg) = TRAIT_COLORS[colorIdx]
                        TraitChip(trait, bg, fg)
                    }
                }
            }
        }
    }
}

@Composable
private fun WalkStatsSection() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WalkStatItem(
                icon = Icons.Outlined.Schedule,
                iconColor = PointGreen,
                label = "평균 산책 시간",
                value = "${MOCK_WALK_MINUTES}분"
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(Color(0xFFE0E0E0))
            )
            WalkStatItem(
                icon = Icons.Outlined.Route,
                iconColor = PointGreen,
                label = "평균 산책 거리",
                value = "${MOCK_WALK_KM}km"
            )
        }
    }
}

@Composable
private fun WalkStatItem(icon: ImageVector, iconColor: Color, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Text(label, fontSize = 12.sp, color = TextGray)
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
private fun BadgeSection(
    earnedBadges: List<com.frontend.domain.model.BadgeProgressResponse>,
    onBadgesClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("나의 뱃지", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text(
                "전체보기 >",
                fontSize = 13.sp,
                color = PointGreen,
                modifier = Modifier.clickable(onClick = onBadgesClick)
            )
        }
        if (earnedBadges.isEmpty()) {
            Text(
                "아직 획득한 배지가 없어요",
                fontSize = 13.sp,
                color = TextGray,
                modifier = Modifier.clickable(onClick = onBadgesClick)
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(earnedBadges) { badge ->
                    val drawableRes = when (badge.badgeId) {
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
                    Image(
                        painter = painterResource(drawableRes),
                        contentDescription = badge.badgeName,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBadgesClick),
                        contentScale = ContentScale.Crop
                    )
                }
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
