package com.e108.be.domain.walk.exception;

import com.e108.be.global.error.exception.InvalidGroupException;

// 400 Bad Request - 이미 종료된 산책에 종료 요청 시
public class WalkAlreadyEndedException extends InvalidGroupException {

    public WalkAlreadyEndedException() {
        super("이미 종료된 산책입니다.");
    }
}
