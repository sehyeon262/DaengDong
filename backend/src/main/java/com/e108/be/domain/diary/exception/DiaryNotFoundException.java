package com.e108.be.domain.diary.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class DiaryNotFoundException extends NotFoundGroupException {

    public DiaryNotFoundException() {
        super("일기를 찾을 수 없습니다.");
    }
}
