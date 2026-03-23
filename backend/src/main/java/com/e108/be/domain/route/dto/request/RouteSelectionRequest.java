package com.e108.be.domain.route.dto.request;

import com.e108.be.domain.route.dto.response.RouteType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RouteSelectionRequest {

    @NotNull
    private RouteType selectedType;

    private int selectedDistanceM;

    private List<Long> placeIds;

    private String weatherCondition;

    private Double temperature;
}
