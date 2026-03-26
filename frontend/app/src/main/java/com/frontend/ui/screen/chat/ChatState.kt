package com.frontend.ui.screen.chat

import com.frontend.domain.model.ChatMessageData

data class ChatState(
    val chatRoomId: Long = 0L,
    val myMemberId: Long = 0L,
    val messages: List<ChatMessageData> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isClosed: Boolean = false,   // 채팅방이 CLOSED 상태일 때
    val error: String? = null,
)
