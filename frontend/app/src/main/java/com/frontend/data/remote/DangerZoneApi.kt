package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.DangerZoneApiResponse
import com.frontend.domain.model.ReportDangerZoneRequest
import com.frontend.domain.model.RiskReportData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface DangerZoneApi {

    /** 위험 구역 신고 — POST /api/v1/safety/risk-zones */
    @POST("safety/risk-zones")
    suspend fun reportDangerZone(
        @Header("Authorization") authorization: String,
        @Body request: ReportDangerZoneRequest
    ): ApiResponse<DangerZoneApiResponse>

    /** 주변 위험 구역 조회 — GET /api/v1/safety/risk-zones */
    @GET("safety/risk-zones")
    suspend fun getDangerZones(
        @Header("Authorization") authorization: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusMeters") radiusMeters: Double = 5000.0
    ): ApiResponse<List<RiskReportData>>
}
