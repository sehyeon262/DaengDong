package com.frontend.data.repository

import com.frontend.data.remote.HomeApi
import com.frontend.domain.model.HomeResponse
import javax.inject.Inject

class HomeRepository @Inject constructor(
    private val homeApi: HomeApi
) {
    suspend fun getHomeData(latitude: Double, longitude: Double): Result<HomeResponse> {
        return try {
            val response = homeApi.getHome(latitude, longitude)
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
