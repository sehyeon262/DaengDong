package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.HomeResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface HomeApi {

    @GET("home")
    suspend fun getHome(
        @Header("Authorization") token: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): ApiResponse<HomeResponse>
}
