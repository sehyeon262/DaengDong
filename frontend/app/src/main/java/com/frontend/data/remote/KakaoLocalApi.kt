package com.frontend.data.remote

import com.frontend.domain.model.KakaoCoord2AddressResponse
import com.frontend.domain.model.KakaoSearchResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/** 카카오 로컬 REST API — https://dapi.kakao.com/v2/local/
 *  baseUrl: "https://dapi.kakao.com/"
 *  Authorization: KakaoAK {REST_API_KEY}
 */
interface KakaoLocalApi {

    /** 키워드로 장소 검색 — GET /v2/local/search/keyword.json */
    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("size") size: Int = 10
    ): KakaoSearchResponse

    /** 좌표 → 주소 변환(역지오코딩) — GET /v2/local/geo/coord2address.json
     *  x: 경도(longitude), y: 위도(latitude) */
    @GET("v2/local/geo/coord2address.json")
    suspend fun coord2Address(
        @Header("Authorization") authorization: String,
        @Query("x") longitude: Double,
        @Query("y") latitude: Double
    ): KakaoCoord2AddressResponse
}
