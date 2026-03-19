package com.e108.be.domain.place.exception;

import com.e108.be.global.error.exception.ConflictGroupException;

public class PlaceDuplicateException extends ConflictGroupException {
    public PlaceDuplicateException() {
        super("이미 등록된 장소입니다.");
    }
}
