package com.e108.be.domain.chat.controller;

import com.e108.be.domain.chat.dto.request.SendMessageRequest;
import com.e108.be.domain.chat.dto.response.ChatMessageResponse;
import com.e108.be.domain.chat.dto.response.ChatRoomResponse;
import com.e108.be.domain.chat.service.ChatService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅방 정보 + 이전 메시지 조회
     * GET /api/v1/chat/rooms/{chatRoomId}
     */
    @GetMapping("/rooms/{chatRoomId}")
    public ResTemplate<ChatRoomResponse> getChatRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal Long memberId) {
        ChatRoomResponse response = chatService.getChatRoom(chatRoomId, memberId);
        return ResTemplate.success(HttpStatus.OK, "채팅방 조회 성공", response);
    }

    /**
     * WebSocket 메시지 전송
     * SEND /app/chat/{chatRoomId}
     * 구독: /topic/chat/{chatRoomId}
     */
    @MessageMapping("/chat/{chatRoomId}")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            SendMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        Long senderId = (Long) headerAccessor.getSessionAttributes().get("memberId");
        if (senderId == null) {
            log.warn("[ChatController] 인증되지 않은 WebSocket 메시지: chatRoomId={}", chatRoomId);
            return;
        }
        ChatMessageResponse response = chatService.saveMessage(chatRoomId, senderId, request.getContent());
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, response);
    }
}
