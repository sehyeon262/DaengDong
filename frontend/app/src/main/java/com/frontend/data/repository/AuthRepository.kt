package com.frontend.data.repository

import android.util.Log
import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.AuthApi
import com.frontend.domain.model.LoginRequest
import com.frontend.domain.model.LoginResponse
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
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
            } catch (e: IOException) {
                // 네트워크 연결 실패 — 서버 로그아웃은 못했지만 로컬 토큰은 반드시 삭제
                Log.w(TAG, "로그아웃 중 네트워크 오류 (로컬 토큰은 삭제됨): ${e.message}")
            } catch (e: HttpException) {
                // 서버 응답 오류 (4xx/5xx) — 이미 만료된 토큰 등 정상 케이스 포함
                Log.w(TAG, "로그아웃 서버 응답 오류 ${e.code()} (로컬 토큰은 삭제됨): ${e.message()}")
            }
        }
        tokenDataStore.clearTokens()
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
