package com.e108.be.domain.record.service;

import com.e108.be.domain.record.dto.response.CalendarResponse;
import com.e108.be.domain.record.dto.response.DailyRecordResponse;
import com.e108.be.domain.record.dto.response.RecordDetailResponse;
import com.e108.be.domain.record.exception.RecordNotFoundException;
import com.e108.be.domain.record.repository.RecordRepository;
import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    private final RecordRepository recordRepository;

    /**
     * 월별 캘린더 조회
     * - 월 통계 (산책률, 총 시간, 총 거리)
     * - 날짜별 산책 횟수
     */
    public CalendarResponse getCalendar(Long dogId, int year, int month) {
        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = YearMonth.of(year, month).atEndOfMonth().plusDays(1).atStartOfDay();

        // 날짜별 산책 횟수
        List<Object[]> dailyCounts = recordRepository.findDailyWalkCounts(dogId, start, end);
        List<CalendarResponse.DayWalkCount> days = dailyCounts.stream()
                .map(row -> CalendarResponse.DayWalkCount.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate())
                        .walkCount(((Number) row[1]).intValue())
                        .build())
                .collect(Collectors.toList());

        // 월별 통계 (단일 집계 row)
        List<Object[]> summaryResult = recordRepository.findMonthlySummary(dogId, start, end);
        Object[] summaryRow = summaryResult.get(0);
        long totalSeconds = ((Number) summaryRow[0]).longValue();
        double totalMeters = ((Number) summaryRow[1]).doubleValue();

        // 산책률: 산책한 날 수 / 해당 월 총 일수 × 100
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        int walkDays = days.size();
        int walkRate = daysInMonth > 0 ? (int) Math.round((double) walkDays / daysInMonth * 100) : 0;

        CalendarResponse.CalendarSummary summary = CalendarResponse.CalendarSummary.builder()
                .walkRate(walkRate)
                .totalDurationMinutes((int) (totalSeconds / 60))
                .totalDistanceKm(Math.round(totalMeters / 10.0) / 100.0) // m → km, 소수점 2자리
                .build();

        return CalendarResponse.builder()
                .year(year)
                .month(month)
                .summary(summary)
                .days(days)
                .build();
    }

    /**
     * 날짜별 산책 기록 목록 조회
     */
    public DailyRecordResponse getDailyRecords(Long dogId, LocalDate date) {
        List<WalkRecord> records = recordRepository.findByDogIdAndDate(dogId, date.atStartOfDay());

        List<DailyRecordResponse.WalkItem> walks = records.stream()
                .map(DailyRecordResponse.WalkItem::from)
                .collect(Collectors.toList());

        return DailyRecordResponse.builder()
                .date(date)
                .walks(walks)
                .build();
    }

    /**
     * 산책 기록 상세 조회
     */
    public RecordDetailResponse getRecordDetail(Long recordId) {
        WalkRecord record = recordRepository.findById(recordId)
                .filter(r -> r.getWalkStatus() == WalkStatus.COMPLETED)
                .orElseThrow(RecordNotFoundException::new);

        return RecordDetailResponse.from(record);
    }
}
