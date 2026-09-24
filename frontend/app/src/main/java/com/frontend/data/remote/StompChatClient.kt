package com.frontend.data.remote

import android.util.Log
import com.frontend.domain.model.ChatMessageData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val BASE_DELAY_MS = 1_000L   // 초기 대기 1초
        const val MAX_DELAY_MS  = 30_000L  // 최대 대기 30초
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var webSocket: WebSocket? = null
    private var subscriptionCounter = 0

    // 재연결에 사용할 접속 정보
    private var savedWsUrl: String? = null
    private var savedToken: String? = null

    // 사용자가 명시적으로 끊었는지 여부 (재연결 방지)
    private var isManualDisconnect = false
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null

    /**
     * chatRoomId별 구독 정보.
     * - subId: STOMP 구독 ID (브로커와 통신할 때 사용)
     * - count: 현재 이 채팅방을 구독 중인 내부 구독자 수 (WalkViewModel, ChatViewModel 등)
     * 브로커 UNSUBSCRIBE는 count가 0이 될 때만 전송한다.
     */
    private data class SubscriptionEntry(val subId: String, val count: Int)
    private val subscriptions = mutableMapOf<Long, SubscriptionEntry>()

    // 수신된 메시지 (chatRoomId → ChatMessageData)
    private val _messages = MutableSharedFlow<Pair<Long, ChatMessageData>>(extraBufferCapacity = 64)
    val messages: SharedFlow<Pair<Long, ChatMessageData>> = _messages.asSharedFlow()

    // STOMP CONNECTED 수신 여부
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    val isConnected: Boolean get() = webSocket != null && _connected.value

    /**
     * 현재 사용자가 열람 중인 채팅방 ID.
     * ChatScreen 진입 시 set, 퇴장 시 null로 초기화.
     * WalkViewModel 배너 알림이 이미 열람 중인 채팅방에 대해 뜨지 않도록 사용.
     */
    var activeChatRoomId: Long? = null

    /**
     * WebSocket 연결 및 STOMP CONNECT 전송.
     * @param wsUrl  ws://host:port/ws-native 형태
     * @param token  JWT 액세스 토큰 (Bearer 접두사 없이)
     */
    fun connect(wsUrl: String, token: String) {
        savedWsUrl = wsUrl
        savedToken = token
        isManualDisconnect = false
        reconnectAttempts = 0
        connectInternal()
    }

    private fun connectInternal() {
        if (webSocket != null) return
        val url   = savedWsUrl  ?: return
        val token = savedToken  ?: return

        val request = Request.Builder().url(url).build()
        val wsClient = okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket 연결 성공")
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
                scheduleReconnectIfNeeded()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 종료 요청 수신: code=$code, reason=$reason")
                _connected.value = false
                // 상대의 Close 프레임에 응답해야 종료가 완료되고 onClosed로 이어진다.
                // 재연결 예약은 기존 onClosed에서 처리한다.
                webSocket.close(code, reason)
            }


            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 종료: code=$code, reason=$reason")
                _connected.value = false
                this@StompChatClient.webSocket = null
                scheduleReconnectIfNeeded()
            }
        })
    }

    /**
     * Exponential Backoff 재연결 스케줄링.
     * 사용자가 명시적으로 끊었거나 최대 횟수 초과 시 중단.
     */
    private fun scheduleReconnectIfNeeded() {
        if (isManualDisconnect) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "최대 재연결 시도 횟수($MAX_RECONNECT_ATTEMPTS) 초과, 재연결 중단")
            return
        }

        // 2^n * 1초 (1s → 2s → 4s → 8s → 16s, 최대 30s)
        val delayMs = minOf(BASE_DELAY_MS * (1L shl reconnectAttempts), MAX_DELAY_MS)
        reconnectAttempts++
        Log.d(TAG, "재연결 예약: $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS 시도, ${delayMs}ms 후")

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delayMs)
            Log.d(TAG, "재연결 시도 중...")
            connectInternal()
        }
    }

    /**
     * 채팅방 구독 (/topic/chat/{chatRoomId}).
     * 이미 브로커 구독이 있으면 내부 카운트만 증가시키고 중복 SUBSCRIBE 전송을 방지한다.
     */
    fun subscribe(chatRoomId: Long) {
        val existing = subscriptions[chatRoomId]
        if (existing != null) {
            subscriptions[chatRoomId] = existing.copy(count = existing.count + 1)
            Log.d(TAG, "채팅방 $chatRoomId 구독자 수 증가: ${existing.count + 1}")
            return
        }
        val subId = "sub-${subscriptionCounter++}"
        subscriptions[chatRoomId] = SubscriptionEntry(subId = subId, count = 1)
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
     * 채팅방 구독 해제.
     * 내부 구독자 카운트를 감소시키고, 0이 될 때만 브로커에 UNSUBSCRIBE를 전송한다.
     */
    fun unsubscribe(chatRoomId: Long) {
        val existing = subscriptions[chatRoomId] ?: return
        if (existing.count > 1) {
            subscriptions[chatRoomId] = existing.copy(count = existing.count - 1)
            Log.d(TAG, "채팅방 $chatRoomId 구독자 수 감소: ${existing.count - 1}")
            return
        }
        subscriptions.remove(chatRoomId)
        val frame = buildStompFrame(
            command = "UNSUBSCRIBE",
            headers = mapOf("id" to existing.subId)
        )
        webSocket?.send(frame)
        Log.d(TAG, "채팅방 $chatRoomId 브로커 구독 해제: ${existing.subId}")
    }

    /** 메시지 전송 (/app/chat/{chatRoomId}) */
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

    /** 명시적 연결 해제 (재연결 없음) */
    fun disconnect() {
        isManualDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempts = 0

        val frame = buildStompFrame(command = "DISCONNECT")
        webSocket?.send(frame)
        webSocket?.close(1000, "정상 종료")
        webSocket = null
        _connected.value = false
        subscriptions.clear()
    }

    // ── 내부 구현 ──────────────────────────────────────────────────────────────

    private fun handleIncoming(raw: String) {
        if (raw.isBlank() || raw == "\n") return  // heartbeat

        val frame = parseStompFrame(raw) ?: return

        when (frame.command) {
            "CONNECTED" -> {
                Log.d(TAG, "STOMP CONNECTED")
                reconnectAttempts = 0   // 연결 성공 시 재시도 카운터 초기화
                _connected.value = true
                // 재연결 시 기존 구독 채팅방들을 자동 재구독 (구독자 수 유지)
                val snapshot = subscriptions.toMap()
                subscriptions.clear()
                snapshot.forEach { (chatRoomId, entry) ->
                    val newSubId = "sub-${subscriptionCounter++}"
                    subscriptions[chatRoomId] = entry.copy(subId = newSubId)
                    val frame = buildStompFrame(
                        command = "SUBSCRIBE",
                        headers = mapOf(
                            "id" to newSubId,
                            "destination" to "/topic/chat/$chatRoomId",
                        )
                    )
                    webSocket?.send(frame)
                    Log.d(TAG, "채팅방 $chatRoomId 재구독: $newSubId (구독자 수: ${entry.count})")
                }
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

    private fun extractChatRoomId(destination: String): Long? =
        destination.substringAfterLast("/").toLongOrNull()

    private fun parseMessageBody(body: String): ChatMessageData? {
        return try {
            val json = JSONObject(body)
            ChatMessageData(
                messageId = json.getLong("messageId"),
                senderId  = json.getLong("senderId"),
                content   = json.getString("content"),
                sentAt    = json.getString("sentAt"),
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
            val text = raw.trimEnd('\u0000')
            val headerBodySplit = text.indexOf("\n\n")
            if (headerBodySplit < 0) return null

            val headerPart = text.substring(0, headerBodySplit)
            val body       = text.substring(headerBodySplit + 2)

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
