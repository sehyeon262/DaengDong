package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.DangerZoneApiResponse
import com.frontend.domain.model.NearbyRiskReportData
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

    /**
     * 주변 위험 구역 조회 (전체 사용자) — GET /api/v1/safety/risk-zones
     * - 지도 전체 표시용 (알림용 아님)
     */
    @GET("safety/risk-zones")
    suspend fun getDangerZones(
        @Header("Authorization") authorization: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusMeters") radiusMeters: Double = 5000.0
    ): ApiResponse<List<RiskReportData>>

    /**
     * 내 위험 구역 전체 목록 조회 — GET /api/v1/safety/risk-zones/mine
     * - 앱 재진입 시 개인 위험장소 복원용
     * - 영구 저장된 목록
     */
    @GET("safety/risk-zones/mine")
    suspend fun getMyRiskZones(
        @Header("Authorization") authorization: String
    ): ApiResponse<List<RiskReportData>>

    /**
     * 내 주변 위험 구역 조회 (거리 포함) — GET /api/v1/safety/risk-zones/nearby
     * - 산책 중 근접 알림 입력 용도
     * - distanceM 포함
     */
    @GET("safety/risk-zones/nearby")
    suspend fun getNearbyMyRiskZones(
        @Header("Authorization") authorization: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusMeters") radiusMeters: Double = 200.0
    ): ApiResponse<List<NearbyRiskReportData>>
}
