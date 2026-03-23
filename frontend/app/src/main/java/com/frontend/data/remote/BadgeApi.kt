package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.BadgeProgressResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface BadgeApi {

    @GET("badges/progress")
    suspend fun getBadgeProgress(
        @Header("Authorization") authorization: String
    ): ApiResponse<List<BadgeProgressResponse>>
}
