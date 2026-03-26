package com.e108.be.domain.chat.exception;

import com.e108.be.global.error.exception.InvalidGroupException;

public class ChatRoomClosedException extends InvalidGroupException {

    public ChatRoomClosedException() {
        super("산책이 종료되어 채팅이 불가합니다.");
    }
}
