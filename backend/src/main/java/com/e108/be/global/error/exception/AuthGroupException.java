package com.e108.be.global.error.exception;

// 401 Unauthorized - 인증 실패
public abstract class AuthGroupException extends RuntimeException {
    public AuthGroupException(String message) {
        super(message);
    }
}
