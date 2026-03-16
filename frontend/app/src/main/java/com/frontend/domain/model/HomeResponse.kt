package com.frontend.domain.model

data class HomeResponse(
    val user: HomeUserInfo?,
    val location: HomeLocationInfo?,
    val weather: HomeWeatherInfo?,
    val walk: HomeWalkInfo?,
    val weeklySummary: HomeWeeklySummary?
)

data class HomeUserInfo(
    val nickname: String,
    val dogName: String,
    val dogProfileImageUrl: String?
)

data class HomeLocationInfo(
    val address: String
)

data class HomeWeatherInfo(
    val weatherCode: String,
    val weatherLabel: String,
    val temperature: Int,
    val temperatureGrade: String,
    val temperatureLabel: String,
    val feelsLike: Int,
    val fineDustValue: Int,
    val fineDustGrade: String,
    val fineDustLabel: String,
    val windSpeed: Double,
    val windGrade: String,
    val windLabel: String
)

data class HomeWalkInfo(
    val walkStatus: String,
    val walkMessage: String,
    val characterType: String
)

data class HomeWeeklySummary(
    val thisWeekTotalDistance: Int,
    val lastWeekTotalDistance: Int,
    val diffDistance: Int,
    val diffMessage: String,
    val weeklyStats: List<DailyWalkStat>?
)

data class DailyWalkStat(
    val day: String,
    val distance: Int
)
