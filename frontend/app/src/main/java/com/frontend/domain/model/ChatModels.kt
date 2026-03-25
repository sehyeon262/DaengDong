package com.frontend.domain.model

data class ChatRoomData(
    val chatRoomId: Long,
    val memberId1: Long,
    val memberId2: Long,
    val status: String,          // "ACTIVE" | "CLOSED"
    val messages: List<ChatMessageData>,
)

data class ChatMessageData(
    val messageId: Long,
    val senderId: Long,
    val content: String,
    val sentAt: String,          // LocalDateTime → String (ISO 형식)
)

data class SendMessageRequest(
    val content: String,
)

/** 채팅 알림 배너용 */
data class ChatBannerNotification(
    val chatRoomId: Long,
    val senderName: String,
    val senderImageUrl: String?,
    val messagePreview: String,
)
