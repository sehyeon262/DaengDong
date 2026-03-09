package com.e108.be.global.error.exception;

// 409 Conflict - 충돌 (중복 등)
public abstract class ConflictGroupException extends RuntimeException {
    public ConflictGroupException(String message) {
        super(message);
    }
}
