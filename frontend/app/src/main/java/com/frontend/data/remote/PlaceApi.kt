package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.Place
import com.frontend.domain.model.PlaceCategory
import retrofit2.http.GET
import retrofit2.http.Header
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
        @Query("radius") radius: Double? = null,   // 검색 반경(m), 기본값 1000, 범위 100~5000
        @Query("limit") limit: Int? = null,         // 최대 결과 수, 기본값 20
        @Query("category") category: String? = null // 카테고리 필터 (예: "카페", "동물병원")
    ): ApiResponse<List<Place>>

    /** 발자국 도장 찍은 장소 목록 조회 — GET /api/v1/maps/stamps */
    @GET("maps/stamps")
    suspend fun getFootprintPlaces(
        @Header("Authorization") authorization: String,
        @Query("dogId") dogId: Long
    ): ApiResponse<List<Place>>
}
