package com.e108.be.domain.walk.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class WalkNotFoundException extends NotFoundGroupException {

    public WalkNotFoundException() {
        super("산책 기록을 찾을 수 없습니다.");
    }
}
