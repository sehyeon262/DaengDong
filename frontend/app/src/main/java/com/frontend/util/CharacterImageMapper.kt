package com.frontend.util

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.frontend.R

@DrawableRes
fun getCharacterImage(characterType: String, temperature: Int = 20, fineDustGrade: String = ""): Int {
    val parts = characterType.split("_")
    val sky = parts.getOrNull(0) ?: ""
    val status = parts.getOrNull(1) ?: ""
    val isDustBad = fineDustGrade in listOf("BAD", "VERY_BAD")

    return when {
        // BAD
        status == "BAD" && sky in listOf("RAINY", "SNOWY") -> R.drawable.rainy
        status == "BAD" && isDustBad -> R.drawable.dust
        status == "BAD" -> R.drawable.cold

        // CAUTION
        status == "CAUTION" && sky in listOf("RAINY", "SNOWY") -> R.drawable.rainy
        status == "CAUTION" && isDustBad -> R.drawable.dust
        // 맑지만 추운 날 (10°C 미만) → cold 이미지
        status == "CAUTION" && sky == "SUNNY" && temperature < 10 -> R.drawable.cold
        // 맑고 폭염 수준 (30°C 이상) → hot 이미지
        status == "CAUTION" && sky == "SUNNY" && temperature >= 28 -> R.drawable.hot
        status == "CAUTION" -> R.drawable.normal

        // GOOD
        status == "GOOD" -> R.drawable.normal

        // GREAT
        status == "GREAT" -> R.drawable.best

        else -> R.drawable.normal
    }
}

fun getWalkStatusColor(walkStatus: String): Color {
    return when (walkStatus) {
        "GREAT" -> Color(0xFF4CAF50)
        "GOOD" -> Color(0xFF2196F3)
        "CAUTION" -> Color(0xFFFF9800)
        "BAD" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

fun getWalkStatusLabel(walkStatus: String): String {
    return when (walkStatus) {
        "GREAT" -> "최상"
        "GOOD" -> "좋음"
        "CAUTION" -> "주의"
        "BAD" -> "나쁨"
        else -> ""
    }
}

fun getWeatherIcon(weatherLabel: String): String {
    return when (weatherLabel) {
        "맑음" -> "sunny"
        "흐림" -> "cloudy"
        "비" -> "rainy"
        "눈" -> "snowy"
        else -> "sunny"
    }
}

fun getGradeColor(grade: String): Color {
    return when (grade) {
        "BEST", "CALM", "COMFORTABLE" -> Color(0xFF4CAF50)
        "GOOD", "COOL", "LIGHT" -> Color(0xFF2196F3)
        "NORMAL" -> Color(0xFFFF9800)
        "BAD", "HOT", "COLD", "STRONG" -> Color(0xFFF44336)
        "VERY_BAD", "VERY_HOT", "VERY_STRONG" -> Color(0xFFD32F2F)
        // 날씨 코드
        "SUNNY" -> Color(0xFF2196F3)
        "CLOUDY" -> Color(0xFF757575)
        "RAINY" -> Color(0xFF1565C0)
        "SNOWY" -> Color(0xFF29B6F6)
        else -> Color.Gray
    }
}
