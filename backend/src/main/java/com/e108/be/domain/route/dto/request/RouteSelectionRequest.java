package com.e108.be.domain.route.dto.request;

import com.e108.be.domain.route.dto.response.RouteType;
import com.e108.be.domain.route.entity.WeatherCondition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RouteSelectionRequest {

    @NotNull
    private RouteType selectedType;

    @Min(0)
    private int selectedDistanceM;

    @Size(max = 20)
    private List<@NotNull Long> placeIds;

    private WeatherCondition weatherCondition;

    private Double temperature;
}
