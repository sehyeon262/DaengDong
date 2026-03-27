package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.Place
import com.frontend.domain.model.PlaceCategory
import com.frontend.domain.model.RegisterPlaceResponse
import com.frontend.domain.model.StampRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaceApi {

    /** 장소 상세정보 조회 — GET /api/v1/places/{placeId} */
    @GET("places/{placeId}")
    suspend fun getPlaceDetail(
        @Header("Authorization") authorization: String,
        @Path("placeId") placeId: Long
    ): ApiResponse<Place>

    /** 장소 카테고리 조회 — GET /api/v1/places/categories */
    @GET("places/categories")
    suspend fun getPlaceCategories(
        @Header("Authorization") authorization: String
    ): ApiResponse<List<PlaceCategory>>

    /** 좌표 기준 주변 장소 조회 — GET /api/v1/places/nearby */
    @GET("places/nearby")
    suspend fun getPlacesNearby(
        @Header("Authorization") authorization: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Double? = null,
        @Query("limit") limit: Int? = null,
        @Query("category") category: String? = null
    ): ApiResponse<List<Place>>

    /** 발자국 도장 찍기 — POST /api/v1/maps/stamps */
    @POST("maps/stamps")
    suspend fun sendStamp(
        @Header("Authorization") authorization: String,
        @Body request: StampRequest
    ): ApiResponse<Unit>

    /** 발자국 도장 찍은 장소 목록 조회 — GET /api/v1/maps/stamps */
    @GET("maps/stamps")
    suspend fun getFootprintPlaces(
        @Header("Authorization") authorization: String,
        @Query("dogId") dogId: Long
    ): ApiResponse<List<Place>>

    /** 발자국 도장 찍기 — POST /api/v1/maps/stamps */
    @POST("maps/stamps")
    suspend fun stampPlace(
        @Header("Authorization") authorization: String,
        @Body request: StampRequest
    ): ApiResponse<Void>

    /** 신규 장소 등록 — POST /api/v1/places (multipart/form-data) */
    @Multipart
    @POST("places")
    suspend fun registerPlace(
        @Header("Authorization") authorization: String,
        @Part("name") name: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("categoryName") categoryName: RequestBody?,
        @Part("address") address: RequestBody?,
        @Part("memo") memo: RequestBody?,
        @Part image: MultipartBody.Part?
    ): ApiResponse<RegisterPlaceResponse>
}
