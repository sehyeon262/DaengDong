package com.e108.be.global.jwt;

/**
 * [JWT 인증 필터]
 * - 모든 요청이 컨트롤러에 도달하기 전에 이 필터를 거침
 * - HTTP 헤더에서 JWT 토큰을 꺼내서 유효한지 검증
 * - 유효하면 → SecurityContext에 인증 정보 저장 (이후 컨트롤러에서 "로그인한 유저"로 인식)
 * - 유효하지 않으면 → 그냥 통과 (인증 없이 진행, 이후 권한 체크에서 막힘)
 *
 * 요청 흐름:
 * HTTP 요청 → JwtAuthenticationFilter → SecurityFilterChain → Controller
 */

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 헤더에서 토큰 꺼내기
        String token = resolveToken(request);

        // 2. 토큰이 있고 유효하면 → 인증 정보 저장
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long memberId = jwtTokenProvider.getMemberId(token);

            // SecurityContext에 인증 정보 저장
            // 이후 컨트롤러에서 @AuthenticationPrincipal 등으로 memberId를 꺼낼 수 있음
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 3. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     * 형식: "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 이후의 토큰 부분만 반환
        }
        return null;
    }
}
