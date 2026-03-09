package com.e108.be.global.error.exception;

// 403 Forbidden - 권한 없음
public abstract class AccessDeniedGroupException extends RuntimeException {
    public AccessDeniedGroupException(String message) {
        super(message);
    }
}
