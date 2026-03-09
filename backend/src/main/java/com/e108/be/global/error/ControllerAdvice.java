package com.e108.be.global.error;

import com.e108.be.global.common.template.ResTemplate;
import com.e108.be.global.error.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerAdvice {

    // 400 - 잘못된 요청 (유효성 검증 실패, 파라미터 오류 등)
    @ExceptionHandler(InvalidGroupException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResTemplate<?> handleBadRequest(RuntimeException e) {
        return ResTemplate.error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 401 - 인증 실패 (로그인 실패, 토큰 만료 등)
    @ExceptionHandler(AuthGroupException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResTemplate<?> handleUnauthorized(RuntimeException e) {
        return ResTemplate.error(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    // 403 - 권한 없음 (로그인은 했지만 접근 권한이 없는 경우)
    @ExceptionHandler(AccessDeniedGroupException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResTemplate<?> handleForbidden(RuntimeException e) {
        return ResTemplate.error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // 404 - 리소스 없음 (회원 없음, 게시글 없음 등)
    @ExceptionHandler(NotFoundGroupException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResTemplate<?> handleNotFound(RuntimeException e) {
        return ResTemplate.error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 409 - 충돌 (이메일 중복, 닉네임 중복 등)
    @ExceptionHandler(ConflictGroupException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResTemplate<?> handleConflict(RuntimeException e) {
        return ResTemplate.error(HttpStatus.CONFLICT, e.getMessage());
    }

    // 500 - 서버 내부 오류 (예상치 못한 에러)
    @ExceptionHandler(InternalServerErrorGroupException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResTemplate<?> handleInternalServerError(RuntimeException e) {
        return ResTemplate.error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
}
