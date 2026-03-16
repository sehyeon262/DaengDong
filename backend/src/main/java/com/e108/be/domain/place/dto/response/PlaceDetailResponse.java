package com.e108.be.domain.place.dto.response;

import com.e108.be.domain.place.entity.Place;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceDetailResponse {

    private Long id;
    private String name;
    private String categoryName;
    private double latitude;
    private double longitude;
    private String address;
    private String contact;
    private String imageUrl;
    private String description;

    public static PlaceDetailResponse from(Place place) {
        return PlaceDetailResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .categoryName(place.getCategory() != null ? place.getCategory().getName() : null)
                .latitude(place.getLocation().getY())
                .longitude(place.getLocation().getX())
                .address(place.getAddress())
                .contact(place.getContact())
                .imageUrl(place.getImageUrl())
                .description(place.getDescription())
                .build();
    }
}
