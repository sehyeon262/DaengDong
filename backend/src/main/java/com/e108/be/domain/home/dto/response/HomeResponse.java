package com.e108.be.domain.home.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeResponse {

    private HomeUserInfo user;
    private HomeLocationInfo location;
    private HomeWeatherInfo weather;
    private HomeWalkInfo walk;
    private HomeWeeklySummary weeklySummary;
}
