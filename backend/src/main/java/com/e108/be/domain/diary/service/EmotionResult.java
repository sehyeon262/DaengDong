package com.e108.be.domain.diary.service;

/**
 * 강아지 감정 분석 결과
 *
 * @param emotion    모델 원본 결과 (angry, happy, relaxed, sad)
 * @param confidence 신뢰도 (0.0 ~ 1.0)
 * @param emotionTag 한글 감정 태그 (즐거움, 평온, 슬픔, 불안)
 */
public record EmotionResult(
        String emotion,
        float confidence,
        String emotionTag
) {
}
