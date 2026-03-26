package com.e108.be.domain.auth.service;

/**
 * [Service 패키지]
 * - 비즈니스 로직(핵심 기능)을 처리하는 계층
 * - Controller에서 요청을 받아 → 로직 수행 → 결과를 돌려줌
 * - DB 접근이 필요하면 Repository를 호출
 * - 예: "이메일로 회원 찾고 → 비밀번호 맞는지 확인하고 → 토큰 발급"
 */

import com.e108.be.domain.auth.dto.request.LoginRequest;
import com.e108.be.domain.auth.dto.request.RefreshTokenRequest;
import com.e108.be.domain.auth.dto.request.RegisterRequest;
import com.e108.be.domain.auth.dto.response.LoginResponse;
import com.e108.be.domain.auth.dto.response.RefreshTokenResponse;
import com.e108.be.domain.auth.dto.response.RegisterResponse;
import com.e108.be.domain.auth.dto.response.ValidateTokenResponse;
import com.e108.be.domain.auth.entity.User;
import com.e108.be.domain.auth.exception.AuthException;
import com.e108.be.domain.auth.exception.EmailDuplicateException;
import com.e108.be.domain.auth.repository.UserRepository;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service //
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final DogRepository dogRepository;

    /**
     * 회원가입 처리 흐름:
     * 1) 이메일 중복 확인
     * 2) 비밀번호 BCrypt 암호화
     * 3) 회원 저장
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailDuplicateException();
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .build();

        User saved = userRepository.save(user);

        return RegisterResponse.builder()
                .userId(saved.getId())
                .email(saved.getEmail())
                .nickname(saved.getNickname())
                .build();
    }

    /**
     * 로그인 처리 흐름:
     * 1) 이메일로 회원 조회 (없으면 예외)
     * 2) 비밀번호 일치 확인 (틀리면 예외)
     * 3) JWT 토큰 생성해서 반환
     */
    public LoginResponse login(LoginRequest request) {
        // 1) 이메일로 회원 찾기
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("존재하지 않는 이메일입니다."));

        // 2) 비밀번호 확인 (입력값 vs DB에 저장된 암호화된 비밀번호)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("비밀번호가 일치하지 않습니다.");
        }

        // 3) JWT 토큰 생성
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        // 4) 해당 유저의 반려견 ID 조회 (없으면 null)
        Long dogId = dogRepository.findFirstByUser_Id(user.getId())
                .map(dog -> dog.getId())
                .orElse(null);

        // 5) 응답 DTO 만들어서 반환
        return LoginResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .dogId(dogId)
                .build();
    }

    /**
     * 토큰 유효성 검증 처리 흐름:
     * 1) Authorization 헤더에서 "Bearer " 제거 후 토큰 추출
     * 2) 토큰 유효성 검증
     * 3) 유효하면 userId 포함해서 반환 / 유효하지 않으면 401 예외
     */
    public ValidateTokenResponse validateToken(String authorizationHeader) {
        // 1) "Bearer " 제거
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AuthException("Authorization 헤더가 올바르지 않습니다.");
        }
        String token = authorizationHeader.substring(7);

        // 2) 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(token)) {
            throw new AuthException("유효하지 않은 토큰입니다.");
        }

        // 3) userId 추출 후 반환
        Long userId = jwtTokenProvider.getMemberId(token);
        return ValidateTokenResponse.builder()
                .isValid(true)
                .userId(userId)
                .build();
    }

    /**
     * Refresh Token으로 새 Access Token + Refresh Token 재발급:
     * 1) Refresh Token 유효성 검증
     * 2) memberId로 유저 조회
     * 3) 새 토큰 발급 후 반환
     */
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthException("유효하지 않은 Refresh Token입니다.");
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new AuthException("존재하지 않는 사용자입니다."));

        String newAccessToken = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * 로그아웃 처리 흐름:
     * 1) Authorization 헤더에서 토큰 추출
     * 2) 토큰 유효성 검증 (유효하지 않으면 401)
     * 3) 정상이면 200 반환 (토큰 삭제는 클라이언트에서 처리)
     * - JWT 특성상 서버에서 토큰을 무효화할 수 없으므로
     *   클라이언트(앱)에서 저장된 토큰을 삭제하는 방식으로 로그아웃 처리
     */
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AuthException("Authorization 헤더가 올바르지 않습니다.");
        }
        String token = authorizationHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new AuthException("유효하지 않은 토큰입니다.");
        }
        // JWT는 stateless이므로 서버 측 처리 없이 클라이언트에서 토큰 삭제로 로그아웃 완료
    }
}
