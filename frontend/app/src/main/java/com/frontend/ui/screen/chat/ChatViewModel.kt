package com.frontend.ui.screen.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.StompChatClient
import com.frontend.data.repository.ChatRepository
import com.frontend.domain.model.ChatMessageData
import com.frontend.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val stompChatClient: StompChatClient,
    private val tokenDataStore: TokenDataStore,
) : ViewModel() {

    private val chatRoomId: Long = checkNotNull(savedStateHandle["chatRoomId"])

    private val _state = MutableStateFlow(ChatState(chatRoomId = chatRoomId))
    val state = _state.asStateFlow()

    init {
        stompChatClient.activeChatRoomId = chatRoomId  // 이 채팅방을 열람 중임을 알림 (배너 억제)
        loadHistory()
        connectAndSubscribe()
        observeMessages()
    }

    // ── 채팅 이력 REST 조회 ────────────────────────────────────────────────────
    private fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            chatRepository.getChatRoom(chatRoomId)
                .onSuccess { room ->
                    _state.update { it.copy(
                        messages = room.messages,
                        isClosed = room.status == "CLOSED",
                        isLoading = false,
                    ) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    // ── WebSocket STOMP 연결 및 구독 ──────────────────────────────────────────
    private fun connectAndSubscribe() {
        viewModelScope.launch {
            val token = tokenDataStore.getAccessToken().first() ?: return@launch
            val myMemberId = extractMemberIdFromJwt(token) ?: 0L
            _state.update { it.copy(myMemberId = myMemberId) }

            val wsUrl = buildWsUrl()
            if (!stompChatClient.isConnected) {
                stompChatClient.connect(wsUrl, token)
                // CONNECTED 신호 대기 (최대 15초) — 연결 실패 시 무한 대기 방지
                val connected = withTimeoutOrNull(15_000L) {
                    stompChatClient.connected.first { it }
                }
                if (connected == null) {
                    _state.update { it.copy(error = "서버 연결에 실패했습니다. 네트워크를 확인해주세요.") }
                    return@launch
                }
            }
            stompChatClient.subscribe(chatRoomId)
        }
    }

    // ── 수신 메시지 관찰 ──────────────────────────────────────────────────────
    private fun observeMessages() {
        viewModelScope.launch {
            stompChatClient.messages.collect { (roomId, message) ->
                if (roomId == chatRoomId) {
                    _state.update { it.copy(messages = it.messages + message) }
                }
            }
        }
    }

    // ── 입력 텍스트 변경 ──────────────────────────────────────────────────────
    fun onInputChanged(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    // ── 메시지 전송 ───────────────────────────────────────────────────────────
    fun sendMessage() {
        val content = _state.value.inputText.trim()
        if (content.isEmpty() || _state.value.isClosed) return

        // 입력창만 즉시 비우고, 실제 메시지는 서버 브로드캐스트로 수신
        _state.update { it.copy(inputText = "") }
        stompChatClient.sendMessage(chatRoomId, content)
    }

    // ── 에러 초기화 ───────────────────────────────────────────────────────────
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // ── JWT에서 memberId 파싱 ────────────────────────────────────────────────
    private fun extractMemberIdFromJwt(token: String): Long? {
        return try {
            val payload = token.split(".").getOrNull(1) ?: return null
            val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
            val json = org.json.JSONObject(String(decoded))
            json.getString("sub").toLongOrNull()
        } catch (_: Exception) {
            null
        }
    }

    // ── WebSocket URL 빌드 ────────────────────────────────────────────────────
    private fun buildWsUrl(): String {
        // BASE_URL 예: "http://192.168.30.183:8080/api/v1/"
        // WS URL 결과: "ws://192.168.30.183:8080/api/v1/ws-native"
        val scheme = if (Constants.BASE_URL.startsWith("https")) "wss" else "ws"
        val base = Constants.BASE_URL
            .removePrefix("https://").removePrefix("http://")
            .trimEnd('/')
        return "$scheme://$base/ws-native"
    }

    override fun onCleared() {
        super.onCleared()
        // 열람 중인 채팅방 초기화 → WalkViewModel 배너 다시 활성화
        if (stompChatClient.activeChatRoomId == chatRoomId) {
            stompChatClient.activeChatRoomId = null
        }
        // WalkViewModel이 동일 chatRoomId를 구독 중일 수 있으므로 브로커 구독은 해제하지 않는다.
        // 구독 해제 시 WalkViewModel의 배너 알림 수신이 중단되는 버그 방지.
    }
}
