package com.e108.be.domain.walk.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class DogNotFoundException extends NotFoundGroupException {

    public DogNotFoundException() {
        super("존재하지 않는 반려견입니다.");
    }
}
