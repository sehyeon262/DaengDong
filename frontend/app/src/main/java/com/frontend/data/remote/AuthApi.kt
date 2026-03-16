package com.frontend.data.remote

import com.frontend.domain.model.LoginRequest
import com.frontend.domain.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse
}