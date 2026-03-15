package com.e108.be.domain.home.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyWalkStat {

    private String day;
    private int distance;
}
