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

    /**
     * StartWalkRequest에서 경로 선택 정보를 추출하여 생성
     */
    public static RouteSelectionRequest of(RouteType selectedType, int selectedDistanceM,
                                            List<Long> placeIds, WeatherCondition weatherCondition,
                                            Double temperature) {
        RouteSelectionRequest request = new RouteSelectionRequest();
        request.selectedType = selectedType;
        request.selectedDistanceM = selectedDistanceM;
        request.placeIds = placeIds;
        request.weatherCondition = weatherCondition;
        request.temperature = temperature;
        return request;
    }
}
