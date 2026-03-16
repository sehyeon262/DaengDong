package com.e108.be.domain.auth.exception;

import com.e108.be.global.error.exception.ConflictGroupException;

// 이메일 중복 → 409 Conflict
public class EmailDuplicateException extends ConflictGroupException {
    public EmailDuplicateException() {
        super("이미 사용 중인 이메일입니다.");
    }
}
