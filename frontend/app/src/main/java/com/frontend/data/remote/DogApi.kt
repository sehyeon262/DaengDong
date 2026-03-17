package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.DogProfileResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface DogApi {

    @GET("dogs/{dogId}")
    suspend fun getDogProfile(
        @Header("Authorization") authorization: String,
        @Path("dogId") dogId: Long
    ): ApiResponse<DogProfileResponse>
}
