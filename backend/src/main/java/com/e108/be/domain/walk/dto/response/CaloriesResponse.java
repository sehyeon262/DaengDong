package com.e108.be.domain.walk.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * GET /walks/{walkId}/calories 응답
 *
 * 체중 있을 때:
 * { "calories": 45.6, "requiresWeight": false }
 *
 * 체중 미입력 시:
 * { "requiresWeight": true }
 */
@Getter
@AllArgsConstructor
public class CaloriesResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double calories;       // 소모 칼로리 (kcal), 체중 없으면 null

    private boolean requiresWeight; // true면 FE에서 체중 입력 유도 UI 표시
}
