package com.e108.be.global.error.exception;

// 400 Bad Request - 잘못된 요청
public abstract class InvalidGroupException extends RuntimeException {
    public InvalidGroupException(String message) {
        super(message);
    }
}
