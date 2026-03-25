package com.frontend.data.remote

import android.util.Log
import com.frontend.domain.model.ChatMessageData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp WebSocket 위에 STOMP 프로토콜을 구현한 채팅 클라이언트.
 * 백엔드의 /ws-native 엔드포인트(SockJS 없이)에 연결한다.
 *
 * STOMP 프레임 형식:
 *   COMMAND\n
 *   header1:value1\n
 *   \n
 *   body\0
 */
@Singleton
class StompChatClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private companion object {
        const val TAG = "StompChatClient"
        const val NULL_BYTE = "\u0000"
    }

    private var webSocket: WebSocket? = null
    private var subscriptionCounter = 0

    // 구독 중인 채팅방 ID → subscription ID 매핑
    private val subscriptions = mutableMapOf<Long, String>()

    // 수신된 메시지 (chatRoomId → ChatMessageData)
    private val _messages = MutableSharedFlow<Pair<Long, ChatMessageData>>(extraBufferCapacity = 64)
    val messages: SharedFlow<Pair<Long, ChatMessageData>> = _messages.asSharedFlow()

    // STOMP CONNECTED 수신 여부 (StateFlow → 현재 값 즉시 접근 가능)
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    /**
     * WebSocket 연결 및 STOMP CONNECT 전송.
     * @param wsUrl  ws://host:port/ws-native 형태
     * @param token  JWT 액세스 토큰 (Bearer 접두사 없이)
     */
    fun connect(wsUrl: String, token: String) {
        if (webSocket != null) return   // 이미 연결됨

        val request = Request.Builder().url(wsUrl).build()
        val wsClient = okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)  // WebSocket은 타임아웃 없음
            .build()

        webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket 연결 성공")
                // STOMP CONNECT 프레임 전송
                val connectFrame = buildStompFrame(
                    command = "CONNECT",
                    headers = mapOf(
                        "Authorization" to "Bearer $token",
                        "accept-version" to "1.1",
                        "heart-beat" to "0,0",
                    )
                )
                webSocket.send(connectFrame)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncoming(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket 오류: ${t.message}")
                _connected.value = false
                this@StompChatClient.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 종료: code=$code, reason=$reason")
                _connected.value = false
                this@StompChatClient.webSocket = null
            }
        })
    }

    /**
     * 특정 채팅방 구독 (/topic/chat/{chatRoomId})
     */
    fun subscribe(chatRoomId: Long) {
        if (subscriptions.containsKey(chatRoomId)) return
        val subId = "sub-${subscriptionCounter++}"
        subscriptions[chatRoomId] = subId
        val frame = buildStompFrame(
            command = "SUBSCRIBE",
            headers = mapOf(
                "id" to subId,
                "destination" to "/topic/chat/$chatRoomId",
            )
        )
        webSocket?.send(frame)
        Log.d(TAG, "채팅방 $chatRoomId 구독: $subId")
    }

    /**
     * 채팅방 구독 해제
     */
    fun unsubscribe(chatRoomId: Long) {
        val subId = subscriptions.remove(chatRoomId) ?: return
        val frame = buildStompFrame(
            command = "UNSUBSCRIBE",
            headers = mapOf("id" to subId)
        )
        webSocket?.send(frame)
    }

    /**
     * 메시지 전송 (/app/chat/{chatRoomId})
     */
    fun sendMessage(chatRoomId: Long, content: String) {
        val body = """{"content":"${content.replace("\"", "\\\"")}"}"""
        val frame = buildStompFrame(
            command = "SEND",
            headers = mapOf(
                "destination" to "/app/chat/$chatRoomId",
                "content-type" to "application/json",
            ),
            body = body,
        )
        webSocket?.send(frame)
    }

    /**
     * WebSocket 연결 해제
     */
    fun disconnect() {
        val frame = buildStompFrame(command = "DISCONNECT")
        webSocket?.send(frame)
        webSocket?.close(1000, "정상 종료")
        webSocket = null
        _connected.value = false
        subscriptions.clear()
    }

    // WebSocket 연결 + STOMP CONNECTED 모두 완료된 상태여야 true
    val isConnected: Boolean get() = webSocket != null && _connected.value

    // ── 내부 구현 ──────────────────────────────────────────────────────────────

    private fun handleIncoming(raw: String) {
        if (raw.isBlank() || raw == "\n") return  // heartbeat

        val frame = parseStompFrame(raw) ?: return

        when (frame.command) {
            "CONNECTED" -> {
                Log.d(TAG, "STOMP CONNECTED")
                _connected.value = true
                // 이미 구독 요청된 방들을 재구독 (재연결 시 대비)
                val pending = subscriptions.keys.toList()
                subscriptions.clear()
                pending.forEach { subscribe(it) }
            }
            "MESSAGE" -> {
                val destination = frame.headers["destination"] ?: return
                val chatRoomId = extractChatRoomId(destination) ?: return
                parseMessageBody(frame.body)?.let { msg ->
                    _messages.tryEmit(chatRoomId to msg)
                }
            }
            "ERROR" -> {
                Log.e(TAG, "STOMP ERROR: ${frame.body}")
            }
        }
    }

    private fun extractChatRoomId(destination: String): Long? {
        // /topic/chat/123 → 123
        return destination.substringAfterLast("/").toLongOrNull()
    }

    private fun parseMessageBody(body: String): ChatMessageData? {
        return try {
            val json = JSONObject(body)
            ChatMessageData(
                messageId = json.getLong("messageId"),
                senderId = json.getLong("senderId"),
                content = json.getString("content"),
                sentAt = json.getString("sentAt"),
            )
        } catch (e: Exception) {
            Log.w(TAG, "메시지 파싱 실패: $body", e)
            null
        }
    }

    // ── STOMP 프레임 빌더 / 파서 ──────────────────────────────────────────────

    private fun buildStompFrame(
        command: String,
        headers: Map<String, String> = emptyMap(),
        body: String = "",
    ): String = buildString {
        append(command).append("\n")
        headers.forEach { (k, v) -> append("$k:$v\n") }
        append("\n")
        append(body)
        append(NULL_BYTE)
    }

    private data class StompFrame(
        val command: String,
        val headers: Map<String, String>,
        val body: String,
    )

    private fun parseStompFrame(raw: String): StompFrame? {
        return try {
            // null byte 제거
            val text = raw.trimEnd('\u0000')
            val headerBodySplit = text.indexOf("\n\n")
            if (headerBodySplit < 0) return null

            val headerPart = text.substring(0, headerBodySplit)
            val body = text.substring(headerBodySplit + 2)

            val lines = headerPart.split("\n")
            if (lines.isEmpty()) return null

            val command = lines[0].trim()
            val headers = lines.drop(1).mapNotNull { line ->
                val colonIdx = line.indexOf(':')
                if (colonIdx < 0) null
                else line.substring(0, colonIdx).trim() to line.substring(colonIdx + 1).trim()
            }.toMap()

            StompFrame(command, headers, body)
        } catch (e: Exception) {
            Log.w(TAG, "STOMP 프레임 파싱 실패: $raw", e)
            null
        }
    }
}
