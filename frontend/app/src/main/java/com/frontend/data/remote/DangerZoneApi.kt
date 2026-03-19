package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.ReportDangerZoneRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DangerZoneApi {

    /** 위험 구역 신고 — POST /api/v1/risk-zones */
    @POST("risk-zones")
    suspend fun reportDangerZone(
        @Header("Authorization") authorization: String,
        @Body request: ReportDangerZoneRequest
    ): ApiResponse<Unit>
}
