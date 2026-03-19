package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.SaveLocationRequest
import com.frontend.domain.model.StartWalkRequest
import com.frontend.domain.model.StartWalkResponse
import com.frontend.domain.model.WalkDetailResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    /** 산책 상세 조회 — GET /api/v1/walks/{walkId} */
    @GET("walks/{walkId}")
    suspend fun getWalkDetail(
        @Header("Authorization") authorization: String,
        @Path("walkId") walkId: Long,
    ): ApiResponse<WalkDetailResponse>

    /** 사진 업로드 — POST /api/v1/walks/{walkId}/photos */
    @Multipart
    @POST("walks/{walkId}/photos")
    suspend fun uploadPhotos(
        @Header("Authorization") authorization: String,
        @Path("walkId") walkId: Long,
        @Part files: List<MultipartBody.Part>,
    ): ApiResponse<List<String>>

    /** 사진 삭제 — DELETE /api/v1/walks/{walkId}/photos */
    @DELETE("walks/{walkId}/photos")
    suspend fun deletePhoto(
        @Header("Authorization") authorization: String,
        @Path("walkId") walkId: Long,
        @Body request: Map<String, String>,
    ): ApiResponse<List<String>>
}
