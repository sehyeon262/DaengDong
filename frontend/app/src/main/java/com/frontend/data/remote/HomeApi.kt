package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.HomeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface HomeApi {

    @GET("home")
    suspend fun getHome(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): ApiResponse<HomeResponse>
}
