package com.e108.be.domain.walk.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

// 산책 세션을 찾을 수 없을 때 → 404 반환
public class WalkNotFoundException extends NotFoundGroupException {

    public WalkNotFoundException() {
        super("산책 세션을 찾을 수 없습니다.");
    }
}
