package com.e108.be.global.logging;

/**
 * [global/logging 패키지]
 * - API 요청/응답 로그를 남기는 곳
 * - 어떤 API가 호출됐는지, 얼마나 걸렸는지 확인할 수 있음
 * - 디버깅할 때 "요청이 서버에 도착은 했나?" 확인하는 데 유용
 *
 * 콘솔 출력 예시:
 * [REQUEST] POST /auth/login
 * [RESPONSE] POST /auth/login - 200 (45ms)
 */

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j      // log.info(), log.error() 사용 가능하게 해줌
@Component  // 스프링이 자동으로 필터로 등록
public class LoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();       // GET, POST 등
        String uri = httpRequest.getRequestURI();      // /auth/login 등

        log.info("[REQUEST] {} {}", method, uri);

        long start = System.currentTimeMillis();
        chain.doFilter(request, response); // 실제 컨트롤러 실행
        long elapsed = System.currentTimeMillis() - start;

        log.info("[RESPONSE] {} {} - {} ({}ms)", method, uri, httpResponse.getStatus(), elapsed);
    }
}
