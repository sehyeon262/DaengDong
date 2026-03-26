package com.frontend.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 토큰 만료 및 인증 실패 시 로그인 화면으로 이동시키기 위한 이벤트 버스.
 * OkHttp Authenticator(백그라운드 스레드)에서 emit하고,
 * NavGraph(UI 스레드)에서 collect하여 화면 전환에 사용.
 */
@Singleton
class UnauthorizedEventBus @Inject constructor() {
    private val _unauthorizedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorizedEvent: SharedFlow<Unit> = _unauthorizedEvent.asSharedFlow()

    fun emit() {
        _unauthorizedEvent.tryEmit(Unit)
    }
}
