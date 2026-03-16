package com.frontend.ui.screen.home.components

import androidx.compose.foundation.background
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.frontend.ui.theme.TextBrown
import com.frontend.ui.theme.TextMain

@Composable
fun HomeHeader(
    location: String,
    dogName: String,
    profileImageUrl: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽: 프로필 이미지 + (위치 + 이름)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = profileImageUrl ?: R.drawable.default_profile,
                contentDescription = "프로필",
                placeholder = painterResource(R.drawable.default_profile),
                error = painterResource(R.drawable.default_profile),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, PointGreen.copy(alpha = 0.3f), CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = location,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBrown
                )
                Text(
                    text = dogName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
            }
        }

        // 오른쪽: 위치 아이콘 (흰색 원형 배경)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.GpsFixed,
                contentDescription = "위치",
                tint = Color(0xFF7A5C5C),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
