package com.e108.be.domain.home.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HomeWeeklySummary {

    private int thisWeekTotalDistance;
    private int lastWeekTotalDistance;
    private int diffDistance;
    private String diffMessage;
    private List<DailyWalkStat> weeklyStats;
}
