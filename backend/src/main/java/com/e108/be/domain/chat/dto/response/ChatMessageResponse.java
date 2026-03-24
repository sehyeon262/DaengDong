package com.e108.be.domain.chat.dto.response;

import com.e108.be.domain.chat.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    private Long messageId;
    private Long senderId;
    private String content;
    private LocalDateTime sentAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .build();
    }
}
