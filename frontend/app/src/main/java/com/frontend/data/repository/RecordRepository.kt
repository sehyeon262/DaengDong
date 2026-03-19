package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.RecordApi
import com.frontend.domain.model.CalendarResponse
import com.frontend.domain.model.DailyRecordResponse
import com.frontend.domain.model.RecordDetailResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RecordRepository @Inject constructor(
    private val recordApi: RecordApi,
    private val tokenDataStore: TokenDataStore
) {

    private suspend fun getDogId(): Long {
        return tokenDataStore.getDogId().first() ?: throw Exception("dogId not found")
    }

    suspend fun getCalendar(year: Int, month: Int): CalendarResponse {
        val dogId = getDogId()
        val response = recordApi.getCalendar(dogId, year, month)
        return response.data ?: throw Exception(response.message)
    }

    suspend fun getDailyRecords(date: String): DailyRecordResponse {
        val dogId = getDogId()
        val response = recordApi.getDailyRecords(dogId, date)
        return response.data ?: throw Exception(response.message)
    }

    suspend fun getRecordDetail(recordId: Long): RecordDetailResponse {
        val response = recordApi.getRecordDetail(recordId)
        return response.data ?: throw Exception(response.message)
    }
}
