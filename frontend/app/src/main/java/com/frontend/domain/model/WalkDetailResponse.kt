package com.frontend.domain.model

data class WalkDetailResponse(
    val walkId: Long,
    val startTime: String,       // "2026-03-12T16:30:00"
    val endTime: String?,
    val durationMinutes: Int,
    val durationSeconds: Int = 0,  // 총 산책 시간(초)
    val distanceKm: Double,
    val calories: Double,
    val photoUrls: List<String>,
    val dogName: String,
    val dogProfileImageUrl: String?,
    val diary: DiaryInfo?
) {
    data class DiaryInfo(
        val diaryId: Long,
        val content: String?,    // null = 생성 중
        val emotionTag: String?, // 대표 감정 태그 (행복, 편안, 슬픔, 화남)
        val photoEmotions: Map<String, String>?, // 사진별 감정 태그 { "url": "행복" }
        val createdAt: String?
    )
}
