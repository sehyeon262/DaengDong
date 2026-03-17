package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.DogProfileResponse
import com.frontend.domain.model.UpdateDogRequest
import com.frontend.domain.model.UpdateTraitsRequest
import com.frontend.domain.model.UpdateWeightRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path

interface DogApi {

    @GET("dogs/{dogId}")
    suspend fun getDogProfile(
        @Header("Authorization") authorization: String,
        @Path("dogId") dogId: Long
    ): ApiResponse<DogProfileResponse>

    @PATCH("dogs/{dogId}")
    suspend fun updateDogProfile(
        @Header("Authorization") authorization: String,
        @Path("dogId") dogId: Long,
        @Body request: UpdateDogRequest
    ): ApiResponse<DogProfileResponse>

    @PATCH("dogs/{dogId}/weight")
    suspend fun updateWeight(
        @Header("Authorization") authorization: String,
        @Path("dogId") dogId: Long,
        @Body request: UpdateWeightRequest
    ): ApiResponse<DogProfileResponse>

    @PATCH("dogs/{dogId}/traits")
    suspend fun updateTraits(
        @Header("Authorization") authorization: String,
        @Path("dogId") dogId: Long,
        @Body request: UpdateTraitsRequest
    ): ApiResponse<Unit>
}
