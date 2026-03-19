package com.e108.be.domain.safety.exception;

import com.e108.be.global.error.exception.AuthGroupException;

// 인증 정보(SecurityContext)가 없을 때 → 401 Unauthorized
public class UnauthenticatedAccessException extends AuthGroupException {

    public UnauthenticatedAccessException() {
        super("인증 정보가 없습니다. 로그인 후 이용해 주세요.");
    }
}
