package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.LoginRequest
import com.frontend.domain.model.LoginResponse
import com.frontend.domain.model.RefreshTokenRequest
import com.frontend.domain.model.RefreshTokenResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<LoginResponse>

    @POST("auth/refresh-token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): ApiResponse<RefreshTokenResponse>

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String
    ): ApiResponse<Void>
}