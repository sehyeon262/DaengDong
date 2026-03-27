package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.AuthApi
import com.frontend.domain.model.LoginRequest
import com.frontend.domain.model.LoginResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenDataStore: TokenDataStore
) {

    suspend fun login(email: String, password: String, keepLogin: Boolean = false): LoginResponse {
        val response = api.login(LoginRequest(email, password))
        val loginData = response.data ?: throw Exception("로그인 응답 데이터가 없습니다")

        tokenDataStore.saveTokens(
            accessToken = loginData.accessToken,
            refreshToken = loginData.refreshToken
        )
        tokenDataStore.saveKeepLogin(keepLogin)
        loginData.dogId?.let { tokenDataStore.saveDogId(it) }

        return loginData
    }

    suspend fun logout() {
        val token = tokenDataStore.getAccessToken().first()
        if (token != null) {
            try {
                api.logout("Bearer $token")
            } catch (_: Exception) {
                // 네트워크 실패해도 로컬 토큰은 반드시 삭제
            }
        }
        tokenDataStore.clearTokens()
    }
}
