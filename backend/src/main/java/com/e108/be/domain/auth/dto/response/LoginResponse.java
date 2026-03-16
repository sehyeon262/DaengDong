package com.e108.be.domain.auth.dto.response;

/**
 * [DTO - Response 패키지]
 * - 서버가 클라이언트(프론트엔드)에게 돌려주는 데이터 형식을 정의
 * - Entity를 직접 반환하지 않고 DTO로 변환하는 이유:
 *   1) 비밀번호 같은 민감한 정보를 제외할 수 있음
 *   2) 프론트에서 필요한 형태로 가공해서 줄 수 있음
 */

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private Long userId;
    private String accessToken;
    private String refreshToken;
}
