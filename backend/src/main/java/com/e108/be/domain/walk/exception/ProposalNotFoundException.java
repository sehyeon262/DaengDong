package com.e108.be.domain.walk.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class ProposalNotFoundException extends NotFoundGroupException {
    public ProposalNotFoundException() {
        super("산책 제안을 찾을 수 없습니다.");
    }
}
