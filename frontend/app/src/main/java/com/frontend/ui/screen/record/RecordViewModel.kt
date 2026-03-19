package com.frontend.ui.screen.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.local.TokenDataStore
import com.frontend.data.repository.DogRepository
import com.frontend.data.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val dogRepository: DogRepository,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(RecordState())
    val state: StateFlow<RecordState> = _state.asStateFlow()

    private var currentYearMonth: YearMonth = YearMonth.now()

    init {
        loadDogInfo()
        loadCalendar()
    }

    private fun loadDogInfo() {
        viewModelScope.launch {
            try {
                val dogId = tokenDataStore.getDogId().first() ?: return@launch
                val dogProfile = dogRepository.getDogProfile(dogId)
                _state.value = _state.value.copy(dogName = dogProfile.name)
            } catch (e: Exception) {
                // 강아지 정보 조회 실패 시 빈 이름으로 유지
            }
        }
    }

    fun loadCalendar(yearMonth: YearMonth = currentYearMonth) {
        currentYearMonth = yearMonth
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val calendar = recordRepository.getCalendar(yearMonth.year, yearMonth.monthValue)
                // 오늘 날짜 또는 산책한 마지막 날짜를 기본 선택
                val today = LocalDate.now()
                val defaultDate = if (yearMonth == YearMonth.now()) {
                    today.toString()
                } else {
                    calendar.days.lastOrNull()?.date
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    calendar = calendar,
                    selectedDate = defaultDate
                )
                // 기본 선택 날짜의 기록 로드
                defaultDate?.let { loadDailyRecords(it) }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "캘린더 조회 실패"
                )
            }
        }
    }

    fun loadDailyRecords(date: String) {
        _state.value = _state.value.copy(selectedDate = date, isDailyLoading = true)
        viewModelScope.launch {
            try {
                val daily = recordRepository.getDailyRecords(date)
                _state.value = _state.value.copy(
                    dailyRecords = daily,
                    isDailyLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    dailyRecords = null,
                    isDailyLoading = false
                )
            }
        }
    }

    fun goToPreviousMonth() {
        loadCalendar(currentYearMonth.minusMonths(1))
    }

    fun goToNextMonth() {
        loadCalendar(currentYearMonth.plusMonths(1))
    }

    fun getCurrentYearMonth(): YearMonth = currentYearMonth
}
