package com.e108.be.domain.place.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class PlaceNotFoundException extends NotFoundGroupException {
    public PlaceNotFoundException() {
        super("장소를 찾을 수 없습니다.");
    }
}
