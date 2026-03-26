package com.e108.be.domain.walk.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class MetDogNotFoundException extends NotFoundGroupException {
    public MetDogNotFoundException() {
        super("만남 기록을 찾을 수 없습니다.");
    }
}
