package com.frontend.di

import com.frontend.data.local.TokenDataStore
import com.frontend.data.local.UnauthorizedEventBus
import com.frontend.data.remote.AuthApi
import com.frontend.domain.model.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator — 서버에서 401 응답이 오면 자동으로 호출됨.
 *
 * - keepLogin == true  → refresh token으로 새 access token 발급 → 요청 재시도
 * - keepLogin == false → DataStore 초기화 → 로그인 화면으로 이동
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenDataStore: TokenDataStore,
    private val unauthorizedEventBus: UnauthorizedEventBus,
    private val authApi: AuthApi   // Auth 전용 Retrofit으로 주입됨 (순환의존 없음)
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 이미 재시도한 요청이라면 무한루프 방지용으로 바로 종료
        if (response.request.header("X-Retry") != null) {
            runBlocking { tokenDataStore.clearTokens() }
            unauthorizedEventBus.emit()
            return null
        }

        val keepLogin = runBlocking { tokenDataStore.getKeepLogin().first() }
        if (!keepLogin) {
            runBlocking { tokenDataStore.clearTokens() }
            unauthorizedEventBus.emit()
            return null
        }

        val refreshToken = runBlocking { tokenDataStore.getRefreshToken().first() }
        if (refreshToken == null) {
            runBlocking { tokenDataStore.clearTokens() }
            unauthorizedEventBus.emit()
            return null
        }

        return try {
            val result = runBlocking { authApi.refreshToken(RefreshTokenRequest(refreshToken)) }
            val newTokens = result.data ?: run {
                runBlocking { tokenDataStore.clearTokens() }
                unauthorizedEventBus.emit()
                return null
            }

            runBlocking {
                tokenDataStore.saveTokens(newTokens.accessToken, newTokens.refreshToken)
            }

            // 원래 요청에 새 토큰 + X-Retry 헤더를 붙여 재시도
            response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .header("X-Retry", "true")
                .build()
        } catch (e: Exception) {
            runBlocking { tokenDataStore.clearTokens() }
            unauthorizedEventBus.emit()
            null
        }
    }
}
