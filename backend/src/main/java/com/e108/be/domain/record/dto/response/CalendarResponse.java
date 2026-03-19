package com.e108.be.domain.record.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class CalendarResponse {

    private int year;
    private int month;
    private CalendarSummary summary;
    private List<DayWalkCount> days;

    @Getter
    @Builder
    public static class CalendarSummary {
        private int walkRate;
        private int totalDurationMinutes;
        private double totalDistanceKm;
    }

    @Getter
    @Builder
    public static class DayWalkCount {
        private LocalDate date;
        private int walkCount;
    }
}
