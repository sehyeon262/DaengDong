package com.e108.be.domain.chat.service;

import com.e108.be.domain.chat.dto.response.ChatMessageResponse;
import com.e108.be.domain.chat.dto.response.ChatRoomResponse;
import com.e108.be.domain.chat.entity.ChatMessage;
import com.e108.be.domain.chat.entity.ChatRoom;
import com.e108.be.domain.chat.entity.ChatRoomStatus;
import com.e108.be.domain.chat.exception.ChatRoomNotFoundException;
import com.e108.be.domain.chat.repository.ChatMessageRepository;
import com.e108.be.domain.chat.repository.ChatRoomRepository;
import com.e108.be.domain.chat.exception.ChatAccessDeniedException;
import com.e108.be.domain.chat.exception.ChatRoomClosedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public Long createChatRoom(Long fromMemberId, Long toMemberId, Long fromWalkRecordId, Long toWalkRecordId) {
        ChatRoom room = ChatRoom.builder()
                .memberId1(fromMemberId)
                .memberId2(toMemberId)
                .walkRecordId1(fromWalkRecordId)
                .walkRecordId2(toWalkRecordId)
                .build();
        return chatRoomRepository.save(room).getId();
    }

    @Transactional
    public void closeRoomByWalkRecordId(Long walkRecordId) {
        List<ChatRoom> rooms = chatRoomRepository.findByWalkRecordIdAndStatus(walkRecordId, ChatRoomStatus.ACTIVE);
        rooms.forEach(ChatRoom::close);
        log.debug("[ChatService] walkRecordId={} 관련 채팅방 {}개 종료", walkRecordId, rooms.size());
    }

    public ChatRoomResponse getChatRoom(Long chatRoomId, Long memberId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(ChatRoomNotFoundException::new);
        validateMember(room, memberId);
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId);
        return ChatRoomResponse.from(room, messages);
    }

    @Transactional
    public ChatMessageResponse saveMessage(Long chatRoomId, Long senderId, String content) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(ChatRoomNotFoundException::new);
        if (room.getStatus() == ChatRoomStatus.CLOSED) {
            throw new ChatRoomClosedException();
        }
        validateMember(room, senderId);
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .senderId(senderId)
                .content(content)
                .build();
        return ChatMessageResponse.from(chatMessageRepository.save(message));
    }

    private void validateMember(ChatRoom room, Long memberId) {
        if (!room.getMemberId1().equals(memberId) && !room.getMemberId2().equals(memberId)) {
            throw new ChatAccessDeniedException();
        }
    }
}
