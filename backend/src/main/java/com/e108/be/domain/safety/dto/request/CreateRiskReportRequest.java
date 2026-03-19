package com.e108.be.domain.safety.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 위험 구역 신고 요청 DTO
 *
 * 유효성 검증은 서비스 레벨(RiskReportService)에서 도메인 예외로 처리합니다.
 * (팀 컨벤션: @Valid + Bean Validation 미사용 — Jackson private 필드 직접 접근 미보장)
 */
@Getter
@NoArgsConstructor
public class CreateRiskReportRequest {

    // 산책 세션 ID — optional (산책 중이 아닐 때도 신고 가능)
    private Long walkSessionId;

    // 위도: -90 ~ 90 (서비스에서 검증)
    private Double latitude;

    // 경도: -180 ~ 180 (서비스에서 검증)
    private Double longitude;

    // 위험 요소 설명 — null/blank 불가 (서비스에서 검증)
    private String description;
}
