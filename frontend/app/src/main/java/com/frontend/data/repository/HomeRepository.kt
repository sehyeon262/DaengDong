package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.HomeApi
import com.frontend.domain.model.HomeResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class HomeRepository @Inject constructor(
    private val homeApi: HomeApi,
    private val tokenDataStore: TokenDataStore
) {
    suspend fun getHomeData(latitude: Double, longitude: Double): Result<HomeResponse> {
        return try {
            val token = tokenDataStore.getAccessToken().first()
                ?: return Result.failure(Exception("로그인이 필요합니다"))
            val response = homeApi.getHome("Bearer $token", latitude, longitude)
            val data = response.data
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
