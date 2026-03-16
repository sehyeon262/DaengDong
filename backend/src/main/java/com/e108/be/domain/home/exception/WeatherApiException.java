package com.e108.be.domain.home.exception;

import com.e108.be.global.error.exception.InternalServerErrorGroupException;

public class WeatherApiException extends InternalServerErrorGroupException {

    public WeatherApiException() {
        super("기상청 날씨 정보를 불러올 수 없습니다.");
    }

    public WeatherApiException(String message) {
        super(message);
    }
}
