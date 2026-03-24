package com.e108.be.domain.chat.exception;

import com.e108.be.global.error.exception.NotFoundGroupException;

public class ChatRoomNotFoundException extends NotFoundGroupException {

    public ChatRoomNotFoundException() {
        super("채팅방을 찾을 수 없습니다.");
    }
}
