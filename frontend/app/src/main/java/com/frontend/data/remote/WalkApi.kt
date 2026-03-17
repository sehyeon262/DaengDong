package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.SaveLocationRequest
import com.frontend.domain.model.StartWalkRequest
import com.frontend.domain.model.StartWalkResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface WalkApi {

    /** W1-01: 산책 시작 — POST /api/v1/walks */
    @POST("walks")
    suspend fun startWalk(
        @Header("Authorization") authorization: String,
        @Body request: StartWalkRequest,
    ): ApiResponse<StartWalkResponse>

    /** W1-02: GPS 좌표 저장 — POST /api/v1/walks/{walkId}/locations */
    @POST("walks/{walkId}/locations")
    suspend fun saveLocation(
        @Header("Authorization") authorization: String,
        @Path("walkId") walkId: Long,
        @Body request: SaveLocationRequest,
    ): ApiResponse<Unit>
}
