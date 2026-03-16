package com.frontend.ui.screen.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frontend.domain.model.HomeWeatherInfo
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain
import com.frontend.util.getGradeColor

@Composable
fun WeatherInfoRow(weatherInfo: HomeWeatherInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeatherInfoCard(
            label = "날씨",
            labelIcon = Icons.Outlined.WbSunny,
            grade = weatherInfo.weatherLabel,
            gradeColor = getGradeColor(weatherInfo.weatherCode),
            bottomIcon = getWeatherIcon(weatherInfo.weatherLabel),
            modifier = Modifier.weight(1f)
        )
        WeatherInfoCard(
            label = "온도",
            labelIcon = Icons.Outlined.Thermostat,
            grade = weatherInfo.temperatureLabel,
            gradeColor = getGradeColor(weatherInfo.temperatureGrade),
            bottomText = "${weatherInfo.temperature}",
            modifier = Modifier.weight(1f)
        )
        WeatherInfoCard(
            label = "대기질",
            labelIcon = Icons.Outlined.Grain,
            grade = weatherInfo.fineDustLabel,
            gradeColor = getGradeColor(weatherInfo.fineDustGrade),
            bottomText = "${weatherInfo.fineDustValue}",
            modifier = Modifier.weight(1f)
        )
        WeatherInfoCard(
            label = "바람",
            labelIcon = Icons.Outlined.Air,
            grade = weatherInfo.windLabel,
            gradeColor = getGradeColor(weatherInfo.windGrade),
            bottomText = "${weatherInfo.windSpeed}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WeatherInfoCard(
    label: String,
    labelIcon: ImageVector,
    grade: String,
    gradeColor: Color,
    bottomText: String = "",
    bottomIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val valueColor = Color(0xFF999999)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, PointGreen.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(top = 10.dp, bottom = 20.dp, start = 8.dp, end = 8.dp)
    ) {
        // 아이콘 + 라벨 왼쪽 상단
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = labelIcon,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextGray
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        // 등급 텍스트 중앙
        Text(
            text = grade,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = gradeColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 값/아이콘 중앙
        if (bottomIcon != null) {
            Icon(
                imageVector = bottomIcon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            Text(
                text = bottomText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun getWeatherIcon(weatherLabel: String): ImageVector {
    return when (weatherLabel) {
        "맑음" -> Icons.Outlined.WbSunny
        "흐림" -> Icons.Outlined.Cloud
        "비" -> Icons.Outlined.WaterDrop
        "눈" -> Icons.Outlined.AcUnit
        else -> Icons.Outlined.WbSunny
    }
}
