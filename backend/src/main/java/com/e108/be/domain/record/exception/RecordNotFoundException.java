package com.e108.be.domain.record.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class RecordNotFoundException extends NotFoundGroupException {
    public RecordNotFoundException() {
        super("산책 기록을 찾을 수 없습니다.");
    }
}
