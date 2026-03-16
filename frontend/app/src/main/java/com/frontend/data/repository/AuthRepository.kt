package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.AuthApi
import com.frontend.domain.model.LoginRequest
import com.frontend.domain.model.LoginResponse
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenDataStore: TokenDataStore
) {

    suspend fun login(email: String, password: String): LoginResponse {
        // 서버에 로그인 요청
        val response = api.login(LoginRequest(email, password))

        // 토큰 로컬에 저장
        tokenDataStore.saveTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken
        )

        // 응답 반환
        return response
    }
}