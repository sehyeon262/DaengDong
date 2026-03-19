package com.e108.be.domain.route.exception;

import com.e108.be.global.error.exception.InvalidGroupException;

public class RouteGenerationException extends InvalidGroupException {
    public RouteGenerationException() {
        super("경로를 생성할 수 없습니다. 주변에 추천할 장소가 부족합니다.");
    }
}
