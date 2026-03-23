package com.e108.be.domain.badge.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class BadgeNotFoundException extends NotFoundGroupException {
    public BadgeNotFoundException() {
        super("뱃지를 찾을 수 없습니다.");
    }
}
