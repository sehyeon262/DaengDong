package com.e108.be.domain.chat.exception;

import com.e108.be.global.error.exception.AccessDeniedGroupException;

public class ChatAccessDeniedException extends AccessDeniedGroupException {

    public ChatAccessDeniedException() {
        super("채팅방 접근 권한이 없습니다.");
    }
}
