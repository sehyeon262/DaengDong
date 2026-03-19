package com.e108.be.domain.safety.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

// 존재하지 않는 산책 세션 참조 → 404 Not Found
public class WalkSessionNotFoundException extends NotFoundGroupException {

    public WalkSessionNotFoundException() {
        super("존재하지 않는 산책 세션입니다.");
    }
}
