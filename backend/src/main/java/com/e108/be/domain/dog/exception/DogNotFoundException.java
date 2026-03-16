package com.e108.be.domain.dog.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class DogNotFoundException extends NotFoundGroupException {
    public DogNotFoundException() {
        super("반려견을 찾을 수 없습니다.");
    }
}
