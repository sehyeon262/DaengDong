package com.frontend.data.remote

import com.frontend.domain.model.ApiResponse
import com.frontend.domain.model.CalendarResponse
import com.frontend.domain.model.DailyRecordResponse
import com.frontend.domain.model.RecordDetailResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RecordApi {

    @GET("records/calendar")
    suspend fun getCalendar(
        @Query("dogId") dogId: Long,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): ApiResponse<CalendarResponse>

    @GET("records/daily")
    suspend fun getDailyRecords(
        @Query("dogId") dogId: Long,
        @Query("date") date: String  // "2026-03-12"
    ): ApiResponse<DailyRecordResponse>

    @GET("records/{recordId}")
    suspend fun getRecordDetail(
        @Path("recordId") recordId: Long
    ): ApiResponse<RecordDetailResponse>
}
