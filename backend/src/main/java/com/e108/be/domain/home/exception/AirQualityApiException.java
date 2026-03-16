package com.e108.be.domain.home.exception;

import com.e108.be.global.error.exception.InternalServerErrorGroupException;

public class AirQualityApiException extends InternalServerErrorGroupException {

    public AirQualityApiException() {
        super("미세먼지 정보를 불러올 수 없습니다.");
    }

    public AirQualityApiException(String message) {
        super(message);
    }
}
