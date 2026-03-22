package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.RouteApi
import com.frontend.domain.model.RouteRecommendResponse
import kotlinx.coroutines.flow.first

class RouteRepository(
    private val routeApi: RouteApi,
    private val tokenDataStore: TokenDataStore
) {

    /**
     * 현재 위치 기반 추천 경로 조회
     */
    suspend fun getRecommendedRoutes(
        latitude: Double,
        longitude: Double
    ): Result<RouteRecommendResponse> = runCatching {
        val token = tokenDataStore.getAccessToken().first() ?: error("인증 토큰 없음")
        routeApi.getRecommendedRoutes(
            authorization = "Bearer $token",
            lat = latitude,
            lon = longitude
        ).data ?: error("경로 데이터 없음")
    }
}
