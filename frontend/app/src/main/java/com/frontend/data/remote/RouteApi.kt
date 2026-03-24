package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.RouteRecommendResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface RouteApi {

    /**
     * 경로 추천 — GET /api/v1/routes/recommend
     * 현재 위치 기반으로 반려견 산책 경로를 추천합니다.
     */
    @GET("routes/recommend")
    suspend fun getRecommendedRoutes(
        @Header("Authorization") authorization: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): ApiResponse<RouteRecommendResponse>
}
