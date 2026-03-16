package com.e108.be.global.jwt;

/**
 * [global/jwt 패키지]
 * - JWT(Json Web Token) 관련 로직을 모아두는 곳
 * - JWT란? 로그인 성공 시 서버가 발급하는 "인증 티켓" 같은 것
 * - 프론트는 이 토큰을 저장했다가, 매 요청마다 Header에 넣어서 보냄
 * - 서버는 토큰을 검증해서 "아, 이 사람 로그인한 사람이구나" 확인
 */

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,       // application.yml에서 주입
            @Value("${jwt.expiration}") long expiration   // 토큰 만료 시간 (밀리초)
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    // 토큰 생성 (로그인 성공 시 호출)
    public String createToken(Long memberId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))       // 토큰에 회원 ID 저장
                .claim("email", email)                   // 토큰에 이메일도 저장
                .issuedAt(now)                           // 발급 시간
                .expiration(new Date(now.getTime() + expiration)) // 만료 시간
                .signWith(secretKey)                     // 서명 (위조 방지)
                .compact();
    }

    public String createRefreshToken(Long memberId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))       // 토큰에 회원 ID 저장
                .claim("email", email)                   // 토큰에 이메일도 저장
                .issuedAt(now)                           // 발급 시간
                .expiration(new Date(now.getTime() + expiration * 336)) // 만료 시간(2주 설정)
                .signWith(secretKey)                     // 서명 (위조 방지)
                .compact();
    }

    // 토큰에서 회원 ID 꺼내기
    public Long getMemberId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("[JWT] 토큰 검증 실패: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
