package com.e108.be.domain.home.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeWeatherInfo {

    private String weatherCode;
    private String weatherLabel;

    private int temperature;
    private String temperatureGrade;
    private String temperatureLabel;
    private int feelsLike;

    private int fineDustValue;
    private String fineDustGrade;
    private String fineDustLabel;

    private double windSpeed;
    private String windGrade;
    private String windLabel;
}
