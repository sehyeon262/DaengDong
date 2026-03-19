package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.LocationBatchRequest
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

    /** R1-03: 자유 산책 시작 — POST /api/v1/walks/free-start */
    @POST("walks/free-start")
    suspend fun startFreeWalk(
        @Header("Authorization") authorization: String,
        @Body request: StartWalkRequest,
    ): ApiResponse<StartWalkResponse>

    /** W1-02: GPS 좌표 배치 저장 — POST /api/v1/walks/{walkId}/locations */
    @POST("walks/{walkId}/locations")
    suspend fun saveLocations(
        @Header("Authorization") authorization: String,
        @Path("walkId") walkId: Long,
        @Body request: LocationBatchRequest,
    ): ApiResponse<Unit>

    /** W1-06: 산책 종료 — POST /api/v1/walks/{walkId}/end */
    @POST("walks/{walkId}/end")
    suspend fun endWalk(
        @Header("Authorization") authorization: String,
        @Path("walkId") walkId: Long,
    ): ApiResponse<Unit>
}
