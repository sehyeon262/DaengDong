package com.e108.be.domain.chat.dto.response;

import com.e108.be.domain.chat.entity.ChatMessageEntry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    private Long senderId;
    private String content;
    private LocalDateTime sentAt;

    public static ChatMessageResponse from(ChatMessageEntry entry) {
        return ChatMessageResponse.builder()
                .senderId(entry.getSenderId())
                .content(entry.getContent())
                .sentAt(entry.getSentAt())
                .build();
    }
}
