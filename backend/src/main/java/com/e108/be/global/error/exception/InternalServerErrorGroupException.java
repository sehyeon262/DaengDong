package com.e108.be.global.error.exception;

// 500 Internal Server Error - 서버 에러
public abstract class InternalServerErrorGroupException extends RuntimeException {
    public InternalServerErrorGroupException(String message) {
        super(message);
    }
}
