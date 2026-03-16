package com.e108.be.domain.place.dto.response;

import com.e108.be.domain.place.entity.PlaceCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceCategoryResponse {

    private Integer id;
    private String name;
    private Integer routeWeight;
    private String iconUrl;

    public static PlaceCategoryResponse from(PlaceCategory category) {
        return PlaceCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .routeWeight(category.getRouteWeight())
                .iconUrl(category.getIconUrl())
                .build();
    }
}
