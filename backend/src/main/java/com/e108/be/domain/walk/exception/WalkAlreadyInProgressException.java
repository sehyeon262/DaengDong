package com.e108.be.domain.walk.exception;

import com.e108.be.global.error.exception.ConflictGroupException;

public class WalkAlreadyInProgressException extends ConflictGroupException {
    public WalkAlreadyInProgressException() {
        super("이미 진행 중인 산책이 있습니다.");
    }
}
