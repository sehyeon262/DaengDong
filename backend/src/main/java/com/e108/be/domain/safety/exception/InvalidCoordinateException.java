package com.e108.be.domain.safety.exception;

import com.e108.be.global.error.exception.InvalidGroupException;

// 좌표 범위 오류 → 400 Bad Request
public class InvalidCoordinateException extends InvalidGroupException {

    public InvalidCoordinateException() {
        super("유효하지 않은 좌표값입니다. 위도는 -90~90, 경도는 -180~180 범위여야 합니다.");
    }

    public InvalidCoordinateException(String message) {
        super(message);
    }
}
