package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.ChatRoomData
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ChatApi {

    /** 채팅방 정보 + 이전 메시지 조회 — GET /api/v1/chat/rooms/{chatRoomId} */
    @GET("chat/rooms/{chatRoomId}")
    suspend fun getChatRoom(
        @Header("Authorization") authorization: String,
        @Path("chatRoomId") chatRoomId: Long,
    ): ApiResponse<ChatRoomData>
}
