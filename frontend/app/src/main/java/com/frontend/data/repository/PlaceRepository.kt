package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.PlaceApi
import com.frontend.domain.model.Place
import com.frontend.domain.model.PlaceCategory
import kotlinx.coroutines.flow.first

class PlaceRepository constructor(
    private val placeApi: PlaceApi,
    private val tokenDataStore: TokenDataStore
) {

    /** 현재 위치 기반 주변 장소 목록 조회 */
    suspend fun getPlacesNearby(
        latitude: Double,
        longitude: Double,
        radius: Double? = null,
        limit: Int? = null,
        category: String? = null
    ): Result<List<Place>> = runCatching {
        val rawToken = tokenDataStore.getAccessToken().first()
        android.util.Log.d("PlaceDebug", "🔑 토큰 조회 결과: ${if (rawToken != null) "있음(${rawToken.take(20)}...)" else "null"}")
        val token = rawToken ?: error("인증 토큰 없음")
        placeApi.getPlacesNearby(
            authorization = "Bearer $token",
            lat = latitude,
            lon = longitude,
            radius = radius,
            limit = limit,
            category = category
        ).data ?: emptyList()
    }

    /** 장소 상세정보 조회 */
    suspend fun getPlaceDetail(placeId: Long): Result<Place> = runCatching {
        val token = tokenDataStore.getAccessToken().first() ?: error("인증 토큰 없음")
        placeApi.getPlaceDetail(
            authorization = "Bearer $token",
            placeId = placeId
        ).data ?: error("장소 데이터 없음")
    }

    /** 장소 카테고리 목록 조회 */
    suspend fun getPlaceCategories(): Result<List<PlaceCategory>> = runCatching {
        val token = tokenDataStore.getAccessToken().first() ?: error("인증 토큰 없음")
        placeApi.getPlaceCategories(
            authorization = "Bearer $token"
        ).data ?: emptyList()
    }
}
