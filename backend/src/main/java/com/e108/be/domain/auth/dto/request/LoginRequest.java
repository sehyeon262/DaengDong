package com.e108.be.domain.auth.dto.request;

/**
 * [DTO - Request 패키지]
 * - 클라이언트(프론트엔드)가 서버로 보내는 데이터 형식을 정의
 * - Entity를 직접 받지 않고 DTO로 받는 이유:
 *   1) 필요한 필드만 받을 수 있음 (Entity에는 id, 생성일 등 불필요한 필드가 있으니까)
 *   2) Entity가 외부에 노출되는 것을 방지
 */

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    private String email;    // 사용자가 입력한 이메일
    private String password; // 사용자가 입력한 비밀번호 (평문)
}
