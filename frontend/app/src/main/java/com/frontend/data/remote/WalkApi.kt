package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.LocationBatchRequest
import com.frontend.domain.model.NearbyDogsResponse
import com.frontend.domain.model.RespondProposalRequest
import com.frontend.domain.model.SaveLocationRequest
import com.frontend.domain.model.SendProposalRequest
import com.frontend.domain.model.StartWalkRequest
import com.frontend.domain.model.StartWalkResponse
import com.frontend.domain.model.FeedbackRequest
import com.frontend.domain.model.MetDogResponse
import com.frontend.domain.model.WalkDetailResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface WalkApi {

    /**
     * 산책 시작 — POST /api/v1/walks
     *
     * - 자유 산책: selectedType = null
     * - 추천 경로 산책: selectedType != null (경로 선택 로그도 서버에서 자동 기록)
     */
    @POST("walks")
    suspend fun startWalk(
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
    ): ApiResponse<com.frontend.domain.model.EndWalkResponse>

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

    /** 만난 친구 목록 조회 — GET /api/v1/walks/met-dogs */
    @GET("walks/met-dogs")
    suspend fun getMetDogs(
        @Header("Authorization") authorization: String,
        @Query("dogId") dogId: Long,
    ): ApiResponse<List<MetDogResponse>>

    /** 궁합 피드백 설정/수정 — PATCH /api/v1/walks/met-dogs/feedback */
    @PATCH("walks/met-dogs/feedback")
    suspend fun updateFeedback(
        @Header("Authorization") authorization: String,
        @Body request: FeedbackRequest,
    ): ApiResponse<Unit>

    /** S14P21E108-171: 함께 산책 제안 전송 — POST /api/v1/walks/proposals */
    @POST("walks/proposals")
    suspend fun sendProposal(
        @Header("Authorization") authorization: String,
        @Body request: SendProposalRequest,
    ): ApiResponse<Map<String, String>>

    /** S14P21E108-171: 산책 제안 수락/거절 — PATCH /api/v1/walks/proposals/{proposalId} */
    @PATCH("walks/proposals/{proposalId}")
    suspend fun respondToProposal(
        @Header("Authorization") authorization: String,
        @Path("proposalId") proposalId: String,
        @Body request: RespondProposalRequest,
    ): ApiResponse<Unit>

    /** W1-03: 주변 강아지 조회 — GET /api/v1/walks/nearby-dogs */
    @GET("walks/nearby-dogs")
    suspend fun getNearbyDogs(
        @Header("Authorization") authorization: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Double = 500.0,
        @Query("myDogId") myDogId: Long,
        @Query("myWalkRecordId") myWalkRecordId: Long,
    ): ApiResponse<NearbyDogsResponse>
}
