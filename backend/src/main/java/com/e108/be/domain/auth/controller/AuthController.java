package com.e108.be.domain.auth.controller;

/**
 * [Controller 패키지]
 * - 클라이언트(프론트엔드)의 HTTP 요청을 받는 진입점
 * - 요청을 받아서 → Service에 넘기고 → 결과를 JSON으로 응답
 * - 비즈니스 로직은 여기에 쓰지 않는다! (Service에 위임)
 *
 * 요청 흐름: 클라이언트 → Controller → Service → Repository → DB
 * 응답 흐름: DB → Repository → Service → Controller → 클라이언트
 */

import com.e108.be.domain.auth.dto.request.LoginRequest;
import com.e108.be.domain.auth.dto.request.RegisterRequest;
import com.e108.be.domain.auth.dto.response.LoginResponse;
import com.e108.be.domain.auth.dto.response.RegisterResponse;
import com.e108.be.domain.auth.dto.response.ValidateTokenResponse;
import com.e108.be.domain.auth.service.AuthService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // JSON을 반환하는 컨트롤러라는 뜻
@RequestMapping("/auth") // 이 컨트롤러의 모든 API는 /auth로 시작
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인 API
     * POST /auth/login
     *
     * 요청 예시 (프론트에서 보내는 JSON):
     * {
     *   "email": "user@example.com",
     *   "password": "1234"
     * }
     *
     * 응답 예시 (서버가 돌려주는 JSON):
     * {
     *   "code": 200,
     *   "message": "로그인 성공",
     *   "data": {
     *     "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     *     "nickname": "홍길동"
     *   }
     * }
     */
    @PostMapping("/register")
    public ResTemplate<RegisterResponse> register(@RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResTemplate.success(HttpStatus.CREATED, "회원가입 성공", response);
    }

    @PostMapping("/login")
    public ResTemplate<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResTemplate.success(HttpStatus.OK, "로그인 성공", response);
    }

    /**
     * 토큰 유효성 검증 API (로그인 상태 유지)
     * GET /auth/validate-token
     * Authorization: Bearer {accessToken}
     */
    @GetMapping("/validate-token")
    public ResTemplate<ValidateTokenResponse> validateToken(
            @RequestHeader("Authorization") String authorization) {
        ValidateTokenResponse response = authService.validateToken(authorization);
        return ResTemplate.success(HttpStatus.OK, "유효한 토큰입니다.", response);
    }

    /**
     * 로그아웃 API
     * POST /auth/logout
     * Authorization: Bearer {accessToken}
     * - JWT는 stateless이므로 서버에서 토큰 무효화 불가
     * - 토큰 유효성만 확인 후 200 반환, 실제 삭제는 클라이언트에서 처리
     */
    @PostMapping("/logout")
    public ResTemplate<Void> logout(
            @RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return ResTemplate.success(HttpStatus.OK, "로그아웃이 완료되었습니다.");
    }
}
