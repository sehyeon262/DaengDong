package com.e108.be.domain.safety.exception;

import com.e108.be.global.error.exception.InvalidGroupException;

// 설명(description) 유효성 오류 → 400 Bad Request
public class InvalidDescriptionException extends InvalidGroupException {

    public InvalidDescriptionException() {
        super("위험 요소 설명은 필수입니다.");
    }

    public InvalidDescriptionException(String message) {
        super(message);
    }
}
