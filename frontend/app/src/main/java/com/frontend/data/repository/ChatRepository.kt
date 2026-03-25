package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.ChatApi
import com.frontend.domain.model.ChatRoomData
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val tokenDataStore: TokenDataStore,
) {
    suspend fun getChatRoom(chatRoomId: Long): Result<ChatRoomData> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        chatApi.getChatRoom("Bearer $token", chatRoomId).data
            ?: throw Exception("채팅방 조회 실패")
    }
}
