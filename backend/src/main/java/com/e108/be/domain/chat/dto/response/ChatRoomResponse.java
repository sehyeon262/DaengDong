package com.e108.be.domain.chat.dto.response;

import com.e108.be.domain.chat.entity.ChatMessage;
import com.e108.be.domain.chat.entity.ChatRoom;
import com.e108.be.domain.chat.entity.ChatRoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomResponse {
    private Long chatRoomId;
    private Long memberId1;
    private Long memberId2;
    private ChatRoomStatus status;
    private List<ChatMessageResponse> messages;

    public static ChatRoomResponse from(ChatRoom room, List<ChatMessage> messages) {
        return ChatRoomResponse.builder()
                .chatRoomId(room.getId())
                .memberId1(room.getMemberId1())
                .memberId2(room.getMemberId2())
                .status(room.getStatus())
                .messages(messages.stream().map(ChatMessageResponse::from).toList())
                .build();
    }
}
