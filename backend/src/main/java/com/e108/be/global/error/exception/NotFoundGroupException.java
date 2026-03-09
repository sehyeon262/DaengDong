package com.e108.be.global.error.exception;

// 404 Not Found - 리소스 없음
public abstract class NotFoundGroupException extends RuntimeException {
    public NotFoundGroupException(String message) {
        super(message);
    }
}
