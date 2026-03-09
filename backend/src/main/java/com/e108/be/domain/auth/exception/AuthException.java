package com.e108.be.domain.auth.exception;

import com.e108.be.global.error.exception.AuthGroupException;

/**
 * 인증 실패 시 사용 → 401 반환
 *
 * ──────────────────────────────────────────────
 * [새 예외 만드는 법]
 *
 * 1. 어떤 HTTP 상태코드인지 결정
 *    - 400: InvalidGroupException    (잘못된 요청)
 *    - 401: AuthGroupException       (인증 실패)
 *    - 403: AccessDeniedGroupException(권한 없음)
 *    - 404: NotFoundGroupException   (리소스 없음)
 *    - 409: ConflictGroupException   (충돌/중복)
 *    - 500: InternalServerErrorGroupException (서버 에러)
 *
 * 2. 해당 그룹 예외를 상속해서 이 패키지에 클래스 생성
 *
 * 3. 예시들:
 *
 *    // 회원 못 찾음 → 404
 *    public class MemberNotFoundException extends NotFoundGroupException {
 *        public MemberNotFoundException() {
 *            super("회원을 찾을 수 없습니다.");
 *        }
 *    }
 *
 *    // 이메일 중복 → 409
 *    public class EmailDuplicateException extends ConflictGroupException {
 *        public EmailDuplicateException() {
 *            super("이미 사용 중인 이메일입니다.");
 *        }
 *    }
 *
 *    // 토큰 만료 → 401
 *    public class TokenExpiredException extends AuthGroupException {
 *        public TokenExpiredException() {
 *            super("토큰이 만료되었습니다.");
 *        }
 *    }
 *
 *    // 비밀번호 형식 오류 → 400
 *    public class InvalidPasswordException extends InvalidGroupException {
 *        public InvalidPasswordException() {
 *            super("비밀번호는 8자 이상이어야 합니다.");
 *        }
 *    }
 *
 * 4. Service에서 throw만 하면 ControllerAdvice가 알아서 처리함
 *    throw new MemberNotFoundException();  → 404 자동 반환
 * ──────────────────────────────────────────────
 */
public class AuthException extends AuthGroupException {

    public AuthException() {
        super("인증에 실패했습니다.");
    }

    public AuthException(String message) {
        super(message);
    }
}
