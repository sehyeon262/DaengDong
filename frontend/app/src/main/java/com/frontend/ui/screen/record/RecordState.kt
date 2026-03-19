package com.frontend.ui.screen.record

import com.frontend.domain.model.CalendarResponse
import com.frontend.domain.model.DailyRecordResponse

data class RecordState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val calendar: CalendarResponse? = null,
    val selectedDate: String? = null,
    val dailyRecords: DailyRecordResponse? = null,
    val isDailyLoading: Boolean = false,
    val dogName: String = "",
    val dogProfileImageUrl: String? = null
)
