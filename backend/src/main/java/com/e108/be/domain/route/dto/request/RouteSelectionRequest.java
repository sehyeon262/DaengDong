package com.e108.be.domain.route.dto.request;

import com.e108.be.domain.route.dto.response.RouteType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RouteSelectionRequest {

    @NotNull
    private RouteType selectedType;

    @Min(-90) @Max(90)
    private double latitude;

    @Min(-180) @Max(180)
    private double longitude;

    // 선택된 경로의 총 거리 (미터)
    private int selectedDistanceM;

    // 선택된 경로의 장소 수
    private int selectedPlaceCount;
}
