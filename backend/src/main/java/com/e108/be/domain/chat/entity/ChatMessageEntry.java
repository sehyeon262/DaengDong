package com.e108.be.domain.chat.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEntry {

    private Long senderId;
    private String content;
    private LocalDateTime sentAt;

    @Builder
    public ChatMessageEntry(Long senderId, String content) {
        this.senderId = senderId;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }
}
